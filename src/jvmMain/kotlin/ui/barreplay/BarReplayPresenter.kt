package ui.barreplay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import trading.Timeframe
import ui.barreplay.launchform.ReplayLaunchFormFields
import ui.barreplay.model.BarReplayEvent
import ui.barreplay.model.BarReplayScreen
import ui.barreplay.model.BarReplayState
import ui.common.CollectEffect
import ui.common.timeframeFromLabel
import ui.common.toLabel
import utils.NIFTY50
import kotlin.time.Duration.Companion.days

internal class BarReplayPresenter(
    coroutineScope: CoroutineScope,
) {

    private val events = MutableSharedFlow<BarReplayEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private var screen by mutableStateOf<BarReplayScreen>(
        BarReplayScreen.LaunchForm(formModel = initialLaunchFormModel())
    )

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is BarReplayEvent.LaunchReplay -> onLaunchReplay(event.formModel)
                BarReplayEvent.NewReplay -> onNewReplay()
            }
        }

        return@launchMolecule BarReplayState(
            currentScreen = screen,
        )
    }

    fun event(event: BarReplayEvent) {
        events.tryEmit(event)
    }

    private fun onLaunchReplay(formModel: ReplayLaunchFormFields.Model) {

        screen = BarReplayScreen.Chart(
            baseTimeframe = when (val timeframeLabel = formModel.baseTimeframe) {
                null -> Timeframe.M5
                else -> timeframeFromLabel(timeframeLabel)
            },
            candlesBefore = formModel.candlesBefore,
            replayFrom = formModel.replayFrom.toInstant(TimeZone.currentSystemDefault()),
            dataTo = formModel.dataTo.toInstant(TimeZone.currentSystemDefault()),
            replayFullBar = formModel.replayFullBar,
            initialSymbol = formModel.initialSymbol ?: error("Invalid symbol!"),
        )
    }

    private fun onNewReplay() {
        screen = BarReplayScreen.LaunchForm(
            formModel = initialLaunchFormModel(),
        )
    }

    private fun initialLaunchFormModel(): ReplayLaunchFormFields.Model {

        val currentTime = Clock.System.now()
        val days30 = 30.days

        val candlesBefore = 200
        val dataTo = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
        val replayFrom = currentTime.minus(days30).toLocalDateTime(TimeZone.currentSystemDefault())

        return ReplayLaunchFormFields.Model(
            baseTimeframe = Timeframe.M5.toLabel(),
            candlesBefore = candlesBefore,
            replayFrom = replayFrom,
            dataTo = dataTo,
            replayFullBar = true,
            initialSymbol = NIFTY50.first(),
        )
    }
}
