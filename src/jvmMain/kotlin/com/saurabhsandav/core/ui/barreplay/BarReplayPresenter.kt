package com.saurabhsandav.core.ui.barreplay

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.barreplay.model.BarReplayEvent
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayState
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayState.NewReplay
import com.saurabhsandav.core.ui.barreplay.newreplayform.NewReplayFormModel
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.timeframeFromLabel
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.utils.NIFTY50
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

@Stable
internal class BarReplayPresenter(
    coroutineScope: CoroutineScope,
) {

    private val formValidator = FormValidator()
    private val formModel = initialLaunchFormModel()

    private val events = MutableSharedFlow<BarReplayEvent>(extraBufferCapacity = Int.MAX_VALUE)
    private var replayState by mutableStateOf<ReplayState>(NewReplay(model = formModel))

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                BarReplayEvent.NewReplay -> onNewReplay()
                BarReplayEvent.LaunchReplay -> onLaunchReplay()
            }
        }

        return@launchMolecule BarReplayState(
            replayState = replayState,
        )
    }

    fun event(event: BarReplayEvent) {
        events.tryEmit(event)
    }

    private fun onLaunchReplay() {

        if (!formValidator.isValid()) return

        val baseTimeframe = when (val timeframeLabel = formModel.baseTimeframe.value) {
            null -> Timeframe.M5
            else -> timeframeFromLabel(timeframeLabel)
        }

        val replayParams = ReplayParams(
            baseTimeframe = baseTimeframe,
            candlesBefore = formModel.candlesBefore.value.toInt(),
            replayFrom = formModel.replayFrom.value.toInstant(TimeZone.currentSystemDefault()),
            dataTo = formModel.dataTo.value.toInstant(TimeZone.currentSystemDefault()),
            replayFullBar = formModel.replayFullBar,
            initialTicker = formModel.initialTicker.value!!,
        )

        replayState = ReplayState.ReplayStarted(replayParams = replayParams)
    }

    private fun onNewReplay() {
        replayState = NewReplay(model = formModel)
    }

    private fun initialLaunchFormModel(): NewReplayFormModel {

        val currentTime = Clock.System.now()
        val days30 = 30.days

        val candlesBefore = 200
        val dataTo = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
        val replayFrom = currentTime.minus(days30).toLocalDateTime(TimeZone.currentSystemDefault())

        return NewReplayFormModel(
            validator = formValidator,
            baseTimeframe = Timeframe.M5.toLabel(),
            candlesBefore = candlesBefore.toString(),
            replayFrom = replayFrom,
            dataTo = dataTo,
            replayFullBar = true,
            initialTicker = NIFTY50.first(),
        )
    }
}
