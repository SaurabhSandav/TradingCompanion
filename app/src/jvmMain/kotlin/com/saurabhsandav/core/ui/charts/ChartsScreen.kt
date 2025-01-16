package com.saurabhsandav.core.ui.charts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import com.saurabhsandav.core.LocalScreensModule
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.CandleDataLoginConfirmed
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.CandleDataLoginDeclined
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.showAsSnackbarsIn
import com.saurabhsandav.core.ui.stockchart.StockCharts
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun ChartsScreen(
    onCloseRequest: () -> Unit,
    chartsHandle: ChartsHandle,
    onOpenTradeReview: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val screensModule = LocalScreensModule.current

    val module = remember { screensModule.chartsModule(scope) }
    val presenter = remember { module.presenter() }
    val state by presenter.state.collectAsState()

    LaunchedEffect(state.eventSink) {
        chartsHandle.events.collect(state.eventSink)
    }

    val chartsState = state.chartsState

    if (chartsState != null) {

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(snackbarHostState) {
            module.uiMessagesState.showAsSnackbarsIn(snackbarHostState)
        }

        StockCharts(
            state = chartsState,
            windowTitle = "Charts",
            onCloseRequest = onCloseRequest,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            customControls = {

                Button(
                    modifier = Modifier.fillMaxSize(),
                    onClick = onOpenTradeReview,
                    content = { Text("Trade Review") },
                )
            },
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
        alwaysOnTop = true,
    ) {

        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.containerPadding),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {

            Text("Fetching candle data needs login. Do you want to proceed?")

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
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
