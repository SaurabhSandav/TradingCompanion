package ui.barreplay

import AppModule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import launchUnit
import trading.CandleSeries
import trading.Timeframe
import trading.barreplay.BarReplay
import trading.barreplay.ReplaySession
import trading.dailySessionStart
import trading.data.CandleRepository
import ui.barreplay.model.BarReplayEvent
import ui.barreplay.model.BarReplayFormFields
import ui.barreplay.model.BarReplayScreen
import ui.barreplay.model.BarReplayState
import ui.common.CollectEffect
import kotlin.time.Duration.Companion.seconds

internal class BarReplayPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) {

    private val events = MutableSharedFlow<BarReplayEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private var screen by mutableStateOf<BarReplayScreen>(BarReplayScreen.LaunchForm)
    private var areReplayControlsEnabled by mutableStateOf(false)

    private val barReplay = BarReplay()
    private var sessionReplayManager: SessionReplayManager? = null
    private var autoNextJob: Job? = null

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is BarReplayEvent.LaunchReplay -> onLaunchReplay(event.fields)
                BarReplayEvent.NewReplay -> onNewReplay()
                BarReplayEvent.Reset -> onReset()
                BarReplayEvent.Next -> onNext()
                is BarReplayEvent.ChangeSymbol -> onChangeSymbol(event.symbol)
                is BarReplayEvent.ChangeTimeframe -> onChangeTimeframe(event.timeframe)
                is BarReplayEvent.ChangeIsAutoNextEnabled -> onChangeIsAutoNextEnabled(event.isAutoNextEnabled)
            }
        }

        return@launchMolecule BarReplayState(
            currentScreen = screen,
            areReplayControlsEnabled = areReplayControlsEnabled,
        )
    }

    fun event(event: BarReplayEvent) {
        events.tryEmit(event)
    }

    private fun onLaunchReplay(fields: BarReplayFormFields) = coroutineScope.launchUnit {

        val chartState = ReplayChartState()

        screen = BarReplayScreen.Chart(chartState)

        sessionReplayManager = createReplaySession(
            chartState = chartState,
            sessionParams = SessionReplayManager.SessionParams(
                symbol = fields.symbol.value ?: error("Invalid symbol!"),
                timeframe = when (fields.timeframe.value) {
                    "1D" -> Timeframe.D1
                    else -> Timeframe.M5
                },
                dataFrom = fields.dataFrom.value.toInstant(TimeZone.currentSystemDefault()),
                dataTo = fields.dataTo.value.toInstant(TimeZone.currentSystemDefault()),
                replayFrom = fields.replayFrom.value.toInstant(TimeZone.currentSystemDefault()),
            ),
        )

        areReplayControlsEnabled = true
    }

    private fun onNewReplay() {
        screen = BarReplayScreen.LaunchForm
        areReplayControlsEnabled = false
        sessionReplayManager = null
    }

    private fun onReset() {
        onChangeIsAutoNextEnabled(false)
        barReplay.reset()
        sessionReplayManager?.reset()
    }

    private fun onNext() {
        barReplay.next()
    }

    private fun onChangeSymbol(symbol: String) = coroutineScope.launchUnit {

        areReplayControlsEnabled = false

        val replayManager = sessionReplayManager!!
        replayManager.coroutineScope.cancel()

        barReplay.removeSession(replayManager.session)

        sessionReplayManager = createReplaySession(
            chartState = replayManager.chartState,
            sessionParams = replayManager.sessionParams.copy(symbol = symbol),
        )

        areReplayControlsEnabled = true
    }

    private fun onChangeTimeframe(newTimeframe: String) = coroutineScope.launchUnit {

        areReplayControlsEnabled = false

        val timeframe = when (newTimeframe) {
            "1D" -> Timeframe.D1
            else -> Timeframe.M5
        }

        val replayManager = sessionReplayManager!!
        replayManager.coroutineScope.cancel()

        sessionReplayManager = replayManager.copy(
            sessionParams = replayManager.sessionParams.copy(timeframe = timeframe),
        )

        areReplayControlsEnabled = true
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

    private suspend fun createReplaySession(
        chartState: ReplayChartState,
        sessionParams: SessionReplayManager.SessionParams,
    ): SessionReplayManager {

        val candleSeries = getCandleSeries(
            symbol = sessionParams.symbol,
            timeframe = sessionParams.timeframe,
            dataFrom = sessionParams.dataFrom,
            dataTo = sessionParams.dataTo,
        )

        val session = barReplay.newSession { currentOffset ->
            ReplaySession(
                inputSeries = candleSeries,
                initialIndex = candleSeries.indexOfFirst { it.openInstant >= sessionParams.replayFrom },
                currentOffset = currentOffset,
                isSessionStart = ::dailySessionStart,
            )
        }

        return SessionReplayManager(
            session = session,
            sessionParams = sessionParams,
            chartState = chartState,
        )
    }

    private suspend fun getCandleSeries(
        symbol: String,
        timeframe: Timeframe,
        dataFrom: Instant,
        dataTo: Instant,
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
}
