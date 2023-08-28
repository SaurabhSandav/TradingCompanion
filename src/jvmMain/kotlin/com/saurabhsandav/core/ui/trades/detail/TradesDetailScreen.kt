package com.saurabhsandav.core.ui.trades.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.trades.detail.model.TradeDetailEvent.*
import com.saurabhsandav.core.ui.trades.detail.ui.MfeAndMae
import com.saurabhsandav.core.ui.trades.detail.ui.Notes
import com.saurabhsandav.core.ui.trades.detail.ui.StopsAndTargets
import com.saurabhsandav.core.ui.trades.detail.ui.TradeDetailItem

@Composable
internal fun TradeDetailWindow(
    profileId: Long,
    tradeId: Long,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { TradeDetailPresenter(profileId, tradeId, scope, appModule) }

    val windowState = rememberWindowState(
        placement = WindowPlacement.Maximized,
    )

    AppWindow(
        state = windowState,
        title = "Trade Detail ($tradeId)",
        onCloseRequest = onCloseRequest,
    ) {

        TradeDetailScreen(presenter)
    }
}

@Composable
internal fun TradeDetailScreen(
    presenter: TradeDetailPresenter,
) {

    val state by presenter.state.collectAsState()

    Scaffold {

        when (val detail = state.tradeDetail) {
            null -> CircularProgressIndicator()
            else -> {

                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {

                    TradeDetailItem(detail)

                    val mfeAndMae = state.mfeAndMae

                    if (mfeAndMae != null) {
                        MfeAndMae(mfeAndMae)
                    }

                    StopsAndTargets(
                        stops = state.stops,
                        onAddStop = { state.eventSink(AddStop(it)) },
                        onDeleteStop = { state.eventSink(DeleteStop(it)) },
                        targets = state.targets,
                        onAddTarget = { state.eventSink(AddTarget(it)) },
                        onDeleteTarget = { state.eventSink(DeleteTarget(it)) },
                    )

                    Notes(
                        notes = state.notes,
                        onAddNote = { state.eventSink(AddNote(it)) },
                        onUpdateNote = { id, note -> state.eventSink(UpdateNote(id, note)) },
                        onDeleteNote = { state.eventSink(DeleteNote(it)) },
                    )
                }
            }
        }
    }
}
