package com.saurabhsandav.core.ui.barreplay

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.barreplay.model.BarReplayEvent
import com.saurabhsandav.core.ui.barreplay.model.BarReplayEvent.*
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayState
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayState.NewReplay
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayState.ReplayStarted
import com.saurabhsandav.core.ui.barreplay.newreplayform.NewReplayFormModel
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.utils.NIFTY50
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

@Stable
internal class BarReplayPresenter(
    private val coroutineScope: CoroutineScope,
    appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
) {

    private val formValidator = FormValidator()

    private var replayState by mutableStateOf<ReplayState?>(null)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule BarReplayState(
            replayState = replayState,
            eventSink = ::onEvent,
        )
    }

    init {

        coroutineScope.launch {
            replayState = NewReplay(model = initialLaunchFormModel())
        }
    }

    private fun onEvent(event: BarReplayEvent) {

        when (event) {
            NewReplay -> onNewReplay()
            LaunchReplay -> onLaunchReplay()
        }
    }

    private fun onLaunchReplay() {

        if (!formValidator.isValid()) return

        val formModel = (replayState as NewReplay).model

        val replayParams = ReplayParams(
            baseTimeframe = formModel.baseTimeframe.value!!,
            candlesBefore = formModel.candlesBefore.value.toInt(),
            replayFrom = formModel.replayFrom.value.toInstant(TimeZone.currentSystemDefault()),
            dataTo = formModel.dataTo.value.toInstant(TimeZone.currentSystemDefault()),
            replayFullBar = formModel.replayFullBar,
            initialTicker = formModel.initialTicker.value!!,
        )

        replayState = ReplayStarted(replayParams = replayParams)
    }

    private fun onNewReplay() = coroutineScope.launchUnit {
        replayState = NewReplay(model = initialLaunchFormModel())
    }

    private suspend fun initialLaunchFormModel(): NewReplayFormModel {

        val defaultTimeframe = appPrefs.getStringFlow(PrefKeys.DefaultTimeframe, PrefDefaults.DefaultTimeframe.name)
            .map(Timeframe::valueOf)
            .first()

        val currentTime = Clock.System.now()
        val days30 = 30.days

        val candlesBefore = 200
        val dataTo = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
        val replayFrom = currentTime.minus(days30).toLocalDateTime(TimeZone.currentSystemDefault())

        return NewReplayFormModel(
            validator = formValidator,
            baseTimeframe = defaultTimeframe,
            candlesBefore = candlesBefore.toString(),
            replayFrom = replayFrom,
            dataTo = dataTo,
            replayFullBar = true,
            initialTicker = NIFTY50.first(),
        )
    }
}
