package com.saurabhsandav.core.ui.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.LocalScreensModule
import com.saurabhsandav.core.ui.common.showAsSnackbarsIn
import com.saurabhsandav.core.ui.stockchart.StockChartDecorationType
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
        chartsHandle.eventsFlow.collect(state.eventSink)
    }

    val chartsState = state.chartsState

    if (chartsState == null) return

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarHostState) {
        module.uiMessagesState.showAsSnackbarsIn(snackbarHostState)
    }

    StockCharts(
        onCloseRequest = onCloseRequest,
        state = chartsState,
        windowTitle = "Charts",
        decorationType = StockChartDecorationType.Charts(onOpenTradeReview = onOpenTradeReview),
        windowDecorator = { content ->

            Box {

                content()

                Box(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    propagateMinConstraints = true,
                    content = { SnackbarHost(snackbarHostState) },
                )
            }
        },
    )
}
