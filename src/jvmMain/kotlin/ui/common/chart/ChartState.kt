package ui.common.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import chart.IChartApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import ui.common.WebViewState

@Composable
fun rememberChartState(chart: IChartApi): ChartState {
    val coroutineScope = rememberCoroutineScope()
    return remember { ChartState(coroutineScope, chart) }
}

class ChartState(
    coroutineScope: CoroutineScope,
    val chart: IChartApi,
) {

    val webViewState = WebViewState(coroutineScope)

    private val javaCallbacks = JavaCallbacks()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        coroutineScope.launch {

            // Configure chart if WebView is ready
            snapshotFlow { webViewState.isReady }.filter { it }.collect {

                // Load chart webpage
                webViewState.load(
                    WebViewState::class.java.getResource("/charts_page/index.html")!!.toExternalForm()
                )

                // On page load, execute chart scripts
                webViewState.loadState.collect { loadState ->
                    if (loadState == WebViewState.LoadState.LOADED) {
                        webViewState.executeScript(
                            """|
                            |function chartCallback(callbackMessage) {
                            |  javaCallbacks.onCallback(callbackMessage)
                            |}
                        """.trimMargin()
                        )
                        webViewState.setMember("javaCallbacks", javaCallbacks)
                        chart.scripts.collect(webViewState::executeScript)
                    }
                }
            }
        }
    }

    inner class JavaCallbacks internal constructor() {

        fun onCallback(callbackMessage: String) {

            // To prevent exceptions being swallowed by JS
            coroutineScope.launch {
                chart.onCallback(callbackMessage)
            }
        }
    }
}
