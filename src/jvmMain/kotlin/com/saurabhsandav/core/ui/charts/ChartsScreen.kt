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
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.CandleFetchLoginCancelled
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.OpenChart
import com.saurabhsandav.core.ui.charts.model.ChartsState.FyersLoginWindow
import com.saurabhsandav.core.ui.charts.tradereview.TradeReviewMarkersProvider
import com.saurabhsandav.core.ui.charts.tradereview.TradeReviewWindow
import com.saurabhsandav.core.ui.common.ErrorSnackbar
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.app.AppWindowOwner
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginWindow
import com.saurabhsandav.core.ui.landing.model.LandingState.TradeWindowParams
import com.saurabhsandav.core.ui.stockchart.StockCharts

@Composable
internal fun ChartsScreen(
    tradeWindowsManager: AppWindowsManager<TradeWindowParams>,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { ChartsPresenter(scope, appModule) }
    val state by presenter.state.collectAsState()

    var showTradeReviewWindow by state { false }
    val tradeReviewWindowOwner = remember { AppWindowOwner() }

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
                    onClick = {
                        showTradeReviewWindow = true
                        tradeReviewWindowOwner.childrenToFront()
                    },
                    content = { Text("Trade Review") },
                )
            },
        )
    }

    // Trade review window
    if (showTradeReviewWindow) {

        AppWindowOwner(tradeReviewWindowOwner) {

            val markersProvider = remember {
                TradeReviewMarkersProvider(appModule)
            }

            DisposableEffect(presenter, markersProvider) {
                presenter.addMarkersProvider(markersProvider)
                onDispose { presenter.removeMarkersProvider(markersProvider) }
            }

            TradeReviewWindow(
                onCloseRequest = { showTradeReviewWindow = false },
                onOpenChart = { ticker, start, end -> state.eventSink(OpenChart(ticker, start, end)) },
                markersProvider = markersProvider,
                tradeWindowsManager = tradeWindowsManager,
            )
        }
    }

    // Fyers login window
    val fyersLoginWindowState = state.fyersLoginWindowState

    if (fyersLoginWindowState is FyersLoginWindow.Open) {

        var showLoginWindow by state { false }

        FetchCandleDataLoginConfirmationDialog(
            onConfirm = { showLoginWindow = true },
            onDismissRequest = { state.eventSink(CandleFetchLoginCancelled) },
        )

        if (showLoginWindow) {
            FyersLoginWindow(fyersLoginWindowState.fyersLoginState)
        }
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
