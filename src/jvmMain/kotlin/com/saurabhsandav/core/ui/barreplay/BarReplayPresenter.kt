package com.saurabhsandav.core.ui.barreplay

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.barreplay.model.BarReplayEvent
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayState
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayState.NewReplay
import com.saurabhsandav.core.ui.barreplay.newreplayform.NewReplayFormModel
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.utils.NIFTY50
import kotlinx.coroutines.CoroutineScope
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

    private var replayState by mutableStateOf<ReplayState>(NewReplay(model = formModel))

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule BarReplayState(
            replayState = replayState,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: BarReplayEvent) {

        when (event) {
            BarReplayEvent.NewReplay -> onNewReplay()
            BarReplayEvent.LaunchReplay -> onLaunchReplay()
        }
    }

    private fun onLaunchReplay() {

        if (!formValidator.isValid()) return

        val replayParams = ReplayParams(
            baseTimeframe = formModel.baseTimeframe.value ?: Timeframe.M5,
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
            baseTimeframe = Timeframe.M5,
            candlesBefore = candlesBefore.toString(),
            replayFrom = replayFrom,
            dataTo = dataTo,
            replayFullBar = true,
            initialTicker = NIFTY50.first(),
        )
    }
}
