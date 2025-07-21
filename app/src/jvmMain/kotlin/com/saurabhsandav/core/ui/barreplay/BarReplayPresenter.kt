package com.saurabhsandav.core.ui.barreplay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.di.AppPrefs
import com.saurabhsandav.core.ui.barreplay.model.BarReplayEvent
import com.saurabhsandav.core.ui.barreplay.model.BarReplayEvent.NewReplay
import com.saurabhsandav.core.ui.barreplay.model.BarReplayEvent.SubmitReplayForm
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayState
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayState.ReplayStarted
import com.saurabhsandav.core.ui.barreplay.newreplayform.NewReplayFormModel
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.trading.core.Timeframe
import com.saurabhsandav.trading.market.india.FinvasiaBroker
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@AssistedInject
internal class BarReplayPresenter(
    @Assisted private val coroutineScope: CoroutineScope,
    @AppPrefs private val appPrefs: FlowSettings,
) {

    private var replayState by mutableStateOf<ReplayState?>(null)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule BarReplayState(
            replayState = replayState,
            eventSink = ::onEvent,
        )
    }

    init {

        coroutineScope.launch {
            replayState = ReplayState.NewReplay(model = initialLaunchFormModel())
        }
    }

    private fun onEvent(event: BarReplayEvent) {

        when (event) {
            SubmitReplayForm -> onSubmitReplayForm()
            NewReplay -> onNewReplay()
        }
    }

    private fun onSubmitReplayForm() = coroutineScope.launchUnit {

        val formModel = (replayState as ReplayState.NewReplay).model

        val replayParams = ReplayParams(
            baseTimeframe = formModel.baseTimeframeField.value!!,
            candlesBefore = formModel.candlesBeforeField.value.toInt(),
            replayFrom = formModel.replayFromField.value.toInstant(TimeZone.currentSystemDefault()),
            dataTo = formModel.dataToField.value.toInstant(TimeZone.currentSystemDefault()),
            replayFullBar = formModel.replayFullBar,
            initialSymbolId = formModel.initialSymbolField.value!!,
            profileId = formModel.profileField.value,
        )

        appPrefs.putString(PrefKeys.ReplayFormModel, Json.encodeToString<ReplayParams>(replayParams))

        replayState = ReplayStarted(replayParams = replayParams)
    }

    private fun onNewReplay() = coroutineScope.launchUnit {
        replayState = ReplayState.NewReplay(model = initialLaunchFormModel())
    }

    private suspend fun initialLaunchFormModel(): NewReplayFormModel {

        val replayParams = appPrefs.getStringOrNull(PrefKeys.ReplayFormModel)
            ?.let { Json.decodeFromString<ReplayParams>(it) }

        return when (replayParams) {
            null -> {

                val defaultTimeframe =
                    appPrefs.getStringFlow(PrefKeys.DefaultTimeframe, PrefDefaults.DefaultTimeframe.name)
                        .map(Timeframe::valueOf)
                        .first()

                val currentTime = Clock.System.now()
                val days30 = 30.days

                val candlesBefore = 200
                val dataTo = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
                val replayFrom = currentTime.minus(days30).toLocalDateTime(TimeZone.currentSystemDefault())

                NewReplayFormModel(
                    baseTimeframe = defaultTimeframe,
                    candlesBefore = candlesBefore.toString(),
                    replayFrom = replayFrom,
                    dataTo = dataTo,
                    replayFullBar = true,
                    initialSymbolId = FinvasiaBroker.SymbolIds.NIFTY,
                    profileId = null,
                )
            }

            else -> NewReplayFormModel(
                baseTimeframe = replayParams.baseTimeframe,
                candlesBefore = replayParams.candlesBefore.toString(),
                replayFrom = replayParams.replayFrom.toLocalDateTime(TimeZone.currentSystemDefault()),
                dataTo = replayParams.dataTo.toLocalDateTime(TimeZone.currentSystemDefault()),
                replayFullBar = replayParams.replayFullBar,
                initialSymbolId = replayParams.initialSymbolId,
                profileId = replayParams.profileId,
            )
        }
    }

    @AssistedFactory
    fun interface Factory {

        fun create(coroutineScope: CoroutineScope): BarReplayPresenter
    }
}
