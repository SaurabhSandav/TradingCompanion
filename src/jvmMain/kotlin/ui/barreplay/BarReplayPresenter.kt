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
import ui.barreplay.launchform.ReplayLaunchFormModel
import ui.barreplay.model.BarReplayEvent
import ui.barreplay.model.BarReplayScreen
import ui.barreplay.model.BarReplayState
import ui.common.CollectEffect
import ui.common.form.FormValidator
import ui.common.timeframeFromLabel
import ui.common.toLabel
import utils.NIFTY50
import kotlin.time.Duration.Companion.days

internal class BarReplayPresenter(
    coroutineScope: CoroutineScope,
) {

    private val formValidator = FormValidator()
    private val formModel = initialLaunchFormModel()

    private val events = MutableSharedFlow<BarReplayEvent>(extraBufferCapacity = Int.MAX_VALUE)
    private var screen by mutableStateOf<BarReplayScreen>(BarReplayScreen.LaunchForm(model = formModel))

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                BarReplayEvent.LaunchReplay -> onLaunchReplay()
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

    private fun onLaunchReplay() {

        if (!formValidator.isValid()) return

        screen = BarReplayScreen.Chart(
            baseTimeframe = when (val timeframeLabel = formModel.baseTimeframe.value) {
                null -> Timeframe.M5
                else -> timeframeFromLabel(timeframeLabel)
            },
            candlesBefore = formModel.candlesBefore.value.toInt(),
            replayFrom = formModel.replayFrom.value.toInstant(TimeZone.currentSystemDefault()),
            dataTo = formModel.dataTo.value.toInstant(TimeZone.currentSystemDefault()),
            replayFullBar = formModel.replayFullBar,
            initialSymbol = formModel.initialSymbol.value!!,
        )
    }

    private fun onNewReplay() {
        screen = BarReplayScreen.LaunchForm(model = formModel)
    }

    private fun initialLaunchFormModel(): ReplayLaunchFormModel {

        val currentTime = Clock.System.now()
        val days30 = 30.days

        val candlesBefore = 200
        val dataTo = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
        val replayFrom = currentTime.minus(days30).toLocalDateTime(TimeZone.currentSystemDefault())

        return ReplayLaunchFormModel(
            validator = formValidator,
            baseTimeframe = Timeframe.M5.toLabel(),
            candlesBefore = candlesBefore.toString(),
            replayFrom = replayFrom,
            dataTo = dataTo,
            replayFullBar = true,
            initialSymbol = NIFTY50.first(),
        )
    }
}
