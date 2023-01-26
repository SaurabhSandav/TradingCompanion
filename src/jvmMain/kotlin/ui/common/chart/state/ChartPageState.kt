package ui.common.chart.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.IntSize
import chart.IChartApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import ui.common.WebViewState
import ui.common.chart.arrangement.ChartArrangement

@Stable
fun ChartPageState(
    coroutineScope: CoroutineScope,
    chart: IChartApi,
) = ChartPageState(coroutineScope).apply { connect(chart) }

@Stable
class ChartPageState(
    val coroutineScope: CoroutineScope,
    val arrangement: ChartArrangement? = null,
) {

    val webViewState: WebViewState = WebViewState(
        coroutineScope = coroutineScope,
        isFocusable = false,
    )

    private val scripts = Channel<String>(Channel.UNLIMITED)
    private val charts = mutableListOf<IChartApi>()
    private var currentSize = IntSize.Zero

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
                        webViewState.setMember("state", this@ChartPageState)
                        scripts.consumeAsFlow().collect(webViewState::executeScript)
                    }
                }
            }
        }

        // Send arrangement js scripts to web engine
        if (arrangement != null) {
            coroutineScope.launch {
                arrangement.scripts.collect(scripts::trySend)
            }
        }
    }

    @Suppress("unused")
    fun onCallback(message: String) {
        // Launch in coroutine to prevent exceptions being swallowed by JS
        coroutineScope.launch {
            charts.forEach { it.onCallback(message) }
        }
    }

    fun resize(size: IntSize) {

        // Resize only if necessary
        if (size == currentSize) return

        currentSize = size

        // Resize all charts
        charts.forEach { it.resize(width = size.width, height = size.height) }
    }

    fun connect(
        chart: IChartApi,
        syncConfig: SyncConfig? = null,
    ) {

        // Cache chart
        charts.add(chart)

        // Set initial size for chart
        chart.resize(width = currentSize.width, height = currentSize.height)

        // Send chart js scripts to web engine
        coroutineScope.launch {
            chart.scripts.collect(scripts::trySend)
        }

        // Sync visible range across charts
        if (syncConfig != null) {
            coroutineScope.launch {
                chart.timeScale.subscribeVisibleTimeRangeChange { range ->

                    // Watch only the current chart
                    if (range == null || !syncConfig.isChartFocused()) return@subscribeVisibleTimeRangeChange

                    // Update all other charts matching syncFilter
                    charts.filter { syncConfig.syncChartWith(it) && it != chart }.forEach {
                        it.timeScale.setVisibleRange(range.from, range.to)
                    }
                }
            }
        }
    }

    fun disconnect(chart: IChartApi) {
        charts.remove(chart)
    }

    class SyncConfig(
        val isChartFocused: () -> Boolean = { false },
        val syncChartWith: (IChartApi) -> Boolean = { true },
    )
}
