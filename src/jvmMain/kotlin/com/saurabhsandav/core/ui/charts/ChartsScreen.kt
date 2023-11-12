package com.saurabhsandav.core.ui.charts

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.CandleDataLoginConfirmed
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.CandleDataLoginDeclined
import com.saurabhsandav.core.ui.common.ErrorSnackbar
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.app.AppWindowManager
import com.saurabhsandav.core.ui.stockchart.StockCharts
import com.saurabhsandav.core.ui.tradereview.TradeReviewWindow

@Composable
internal fun ChartsScreen(
    onCloseRequest: () -> Unit,
    chartsHandle: ChartsHandle,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current

    val module = remember { appModule.chartsModule(scope) }
    val presenter = remember { module.presenter() }
    val state by presenter.state.collectAsState()

    val tradeReviewWindowManager = remember { AppWindowManager() }

    LaunchedEffect(state.eventSink) {
        chartsHandle.events.collect(state.eventSink)
    }

    val chartsState = state.chartsState

    if (chartsState != null) {

        StockCharts(
            state = chartsState,
            windowTitle = "Charts",
            onCloseRequest = onCloseRequest,
            snackbarHost = {

                val snackbarHostState = remember { SnackbarHostState() }

                // Errors
                state.errors.forEach { errorMessage ->

                    ErrorSnackbar(snackbarHostState, errorMessage)
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.animateContentSize().align(Alignment.CenterHorizontally),
                )
            },
            customControls = {

                Button(
                    modifier = Modifier.fillMaxSize(),
                    onClick = tradeReviewWindowManager::openWindow,
                    content = { Text("Trade Review") },
                )
            },
        )
    }

    // Trade review window
    tradeReviewWindowManager.Window {

        TradeReviewWindow(
            onCloseRequest = tradeReviewWindowManager::closeWindow,
            chartsHandle = chartsHandle,
        )
    }

    // Login confirmation dialog
    if (state.showCandleDataLoginConfirmation) {

        FetchCandleDataLoginConfirmationDialog(
            onConfirm = { state.eventSink(CandleDataLoginConfirmed) },
            onDismissRequest = { state.eventSink(CandleDataLoginDeclined) },
        )
    }
}

@Composable
private fun FetchCandleDataLoginConfirmationDialog(
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {

    // Ideally, AlertDialog would be used here instead. But currently, Compose cannot draw on top of Swing components.
    // AlertDialog is drawn in the same window on top of existing content. As the chart composable is a swing
    // composable, the AlertDialog will be invisible.
    AppDialogWindow(
        onCloseRequest = onDismissRequest,
        title = "Do you want to login to fetch candles?",
        state = rememberDialogState(
            size = DpSize(
                width = 400.dp,
                height = 150.dp,
            )
        ),
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {

            Text("Fetching candle data needs login. Do you want to proceed?")

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                Button(onClick = onConfirm) {
                    Text("OK")
                }

                Button(onClick = onDismissRequest) {
                    Text("CANCEL")
                }
            }
        }
    }
}
