package com.saurabhsandav.core.ui.charts

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.LocalScreensModule
import com.saurabhsandav.core.ui.common.showAsSnackbarsIn
import com.saurabhsandav.core.ui.stockchart.StockCharts

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

    if (chartsState == null) return

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
