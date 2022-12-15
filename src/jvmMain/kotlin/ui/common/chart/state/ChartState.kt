package ui.common.chart.state

import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import ui.common.WebViewState

abstract class ChartState(val coroutineScope: CoroutineScope) {

    val webViewState: WebViewState = WebViewState(
        coroutineScope = coroutineScope,
        isFocusable = false,
    )

    abstract fun resize(width: Int, height: Int)

    protected abstract val jsCommands: Flow<String>

    abstract fun onCallback(callbackMessage: String)

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
                            |  state.onCallback(callbackMessage)
                            |}
                        """.trimMargin()
                        )
                        webViewState.setMember("state", this@ChartState)
                        jsCommands.collect(webViewState::executeScript)
                    }
                }
            }
        }
    }
}
