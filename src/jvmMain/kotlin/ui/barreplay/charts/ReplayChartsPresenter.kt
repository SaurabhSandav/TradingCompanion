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
import trading.barreplay.ReplaySession
import trading.dailySessionStart
import trading.data.CandleRepository
import ui.barreplay.charts.model.ReplayChartState
import ui.barreplay.charts.model.ReplayChartsEvent
import ui.barreplay.charts.model.ReplayChartsEvent.*
import ui.barreplay.charts.model.ReplayChartsState
import ui.barreplay.charts.model.ReplayControlsState
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
    private val chartBridge = ReplayChartBridge()
    private var sessionReplayManager: SessionReplayManager? = null
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
            chartState = remember { ReplayChartState(chartBridge.chart) },
        )
    }

    fun event(event: ReplayChartsEvent) {
        events.tryEmit(event)
    }

    init {

        coroutineScope.launch {

            sessionReplayManager = createReplaySession(
                chartState = chartBridge,
                symbol = initialSymbol,
                timeframe = baseTimeframe,
            )

            areReplayControlsEnabled = true
        }
    }

    private fun onReset() {
        onChangeIsAutoNextEnabled(false)
        barReplay.reset()
        sessionReplayManager?.reset()
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

        val replayManager = sessionReplayManager!!
        replayManager.coroutineScope.cancel()

        barReplay.removeSession(replayManager.session)

        sessionReplayManager = createReplaySession(
            chartState = replayManager.chartState,
            symbol = symbol,
            timeframe = replayManager.timeframe,
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

        val replayManager = sessionReplayManager!!
        replayManager.coroutineScope.cancel()

        sessionReplayManager = replayManager.copy(timeframe = timeframe)

        areReplayControlsEnabled = true
    }

    private suspend fun createReplaySession(
        symbol: String,
        timeframe: Timeframe,
        chartState: ReplayChartBridge,
    ): SessionReplayManager {

        val candleSeries = getCandleSeries(
            symbol = symbol,
            timeframe = baseTimeframe,
        )

        val session = barReplay.newSession { currentOffset ->

            val replayFrom = replayFrom

            ReplaySession(
                inputSeries = candleSeries,
                initialIndex = candleSeries.indexOfFirst { it.openInstant >= replayFrom },
                currentOffset = currentOffset,
                isSessionStart = ::dailySessionStart,
            )
        }

        return SessionReplayManager(
            session = session,
            timeframe = timeframe,
            chartState = chartState,
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
