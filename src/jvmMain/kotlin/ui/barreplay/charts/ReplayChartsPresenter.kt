package ui.barreplay.charts

import AppModule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Instant
import launchUnit
import trading.CandleSeries
import trading.Timeframe
import trading.barreplay.BarReplay
import trading.barreplay.BarReplaySession
import trading.dailySessionStart
import trading.data.CandleRepository
import ui.barreplay.charts.model.ReplayChartState
import ui.barreplay.charts.model.ReplayChartsEvent
import ui.barreplay.charts.model.ReplayChartsEvent.*
import ui.barreplay.charts.model.ReplayChartsState
import ui.barreplay.charts.model.ReplayControlsState
import ui.barreplay.charts.ui.ReplayChart
import ui.common.CollectEffect
import kotlin.time.Duration.Companion.seconds

internal class ReplayChartsPresenter(
    private val coroutineScope: CoroutineScope,
    private val baseTimeframe: Timeframe,
    private val dataFrom: Instant,
    private val dataTo: Instant,
    private val replayFrom: Instant,
    private val initialSymbol: String,
    private val appModule: AppModule,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) {

    private val events = MutableSharedFlow<ReplayChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)
    private var areReplayControlsEnabled by mutableStateOf(false)
    private var controlsState by mutableStateOf(ReplayControlsState(initialSymbol, baseTimeframe.toText()))

    private val barReplay = BarReplay()
    private val chart = ReplayChart()
    private var replayDataManager: ReplayDataManager? = null
    private var autoNextJob: Job? = null

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                Reset -> onReset()
                Next -> onNext()
                is ChangeIsAutoNextEnabled -> onChangeIsAutoNextEnabled(event.isAutoNextEnabled)
                is ChangeSymbol -> onChangeSymbol(event.symbol)
                is ChangeTimeframe -> onChangeTimeframe(event.timeframe)
            }
        }

        return@launchMolecule ReplayChartsState(
            areReplayControlsEnabled = areReplayControlsEnabled,
            controlsState = controlsState,
            chartState = remember { ReplayChartState(chart.actualChart) },
        )
    }

    fun event(event: ReplayChartsEvent) {
        events.tryEmit(event)
    }

    init {

        coroutineScope.launch {

            replayDataManager = createReplayDataManager(
                chart = chart,
                symbol = initialSymbol,
                timeframe = baseTimeframe,
            )

            areReplayControlsEnabled = true
        }
    }

    private fun onReset() {
        onChangeIsAutoNextEnabled(false)
        barReplay.reset()
        replayDataManager?.reset()
    }

    private fun onNext() {
        barReplay.next()
    }

    private fun onChangeIsAutoNextEnabled(isAutoNextEnabled: Boolean) {

        autoNextJob = when {
            isAutoNextEnabled -> coroutineScope.launch {
                while (isActive) {
                    delay(1.seconds)
                    barReplay.next()
                }
            }

            else -> {
                autoNextJob?.cancel()
                null
            }
        }
    }

    private fun onChangeSymbol(symbol: String) = coroutineScope.launchUnit {

        areReplayControlsEnabled = false
        controlsState = controlsState.copy(symbol = symbol)

        // Find existing session
        val dataManager = replayDataManager!!

        // Stop watching live candles
        dataManager.unsubscribeLiveCandles()

        // Remove session from BarReplay
        barReplay.removeSession(dataManager.replaySession)

        // Create new session with new data
        replayDataManager = createReplayDataManager(
            chart = dataManager.chart,
            symbol = symbol,
            timeframe = dataManager.timeframe,
        )

        areReplayControlsEnabled = true
    }

    private fun onChangeTimeframe(newTimeframe: String) = coroutineScope.launchUnit {

        areReplayControlsEnabled = false
        controlsState = controlsState.copy(timeframe = newTimeframe)

        val timeframe = when (newTimeframe) {
            "1D" -> Timeframe.D1
            else -> Timeframe.M5
        }

        // Find existing session
        val dataManager = replayDataManager!!

        // Stop watching live candles
        dataManager.unsubscribeLiveCandles()

        // Create new session with existing data but a different timeframe
        replayDataManager = dataManager.copy(timeframe = timeframe)

        areReplayControlsEnabled = true
    }

    private suspend fun createReplayDataManager(
        chart: ReplayChart,
        symbol: String,
        timeframe: Timeframe,
    ): ReplayDataManager {

        // Get candles
        val candleSeries = getCandleSeries(
            symbol = symbol,
            timeframe = baseTimeframe,
        )

        // Create new BarReplaySession
        val session = barReplay.newSession { currentOffset ->

            val replayFrom = replayFrom

            BarReplaySession(
                inputSeries = candleSeries,
                initialIndex = candleSeries.indexOfFirst { it.openInstant >= replayFrom },
                currentOffset = currentOffset,
                isSessionStart = ::dailySessionStart,
            )
        }

        return ReplayDataManager(
            chart = chart,
            replaySession = session,
            timeframe = timeframe,
        )
    }

    private suspend fun getCandleSeries(
        symbol: String,
        timeframe: Timeframe,
    ): CandleSeries {

        val candleSeriesResult = candleRepo.getCandles(
            symbol = symbol,
            timeframe = timeframe,
            from = dataFrom,
            to = dataTo,
        )

        return when (candleSeriesResult) {
            is Ok -> candleSeriesResult.value
            is Err -> when (val error = candleSeriesResult.error) {
                is CandleRepository.Error.AuthError -> error("AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }

    private fun Timeframe.toText(): String = when (this) {
        Timeframe.M1 -> "1m"
        Timeframe.M5 -> "5m"
        Timeframe.D1 -> "1D"
    }
}
