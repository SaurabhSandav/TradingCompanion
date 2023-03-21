package com.saurabhsandav.core.ui.common.chart.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.ui.common.WebViewState
import com.saurabhsandav.core.ui.common.chart.arrangement.ChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.single
import com.saurabhsandav.core.ui.common.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Stable
fun ChartPageState(
    coroutineScope: CoroutineScope,
    chart: IChartApi,
) = ChartPageState(coroutineScope, ChartArrangement.single()).apply { connect(chart) }

@Stable
class ChartPageState(
    val coroutineScope: CoroutineScope,
    val arrangement: ChartArrangement,
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
        coroutineScope.launch {
            arrangement.scripts.collect(scripts::trySend)
        }
    }

    @Suppress("unused")
    fun onCallback(message: String) {
        // Launch in coroutine to prevent exceptions being swallowed by JS
        coroutineScope.launch {
            // If arrangement does not consume callback, forward it to the charts
            if (!arrangement.onCallback(message))
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

    fun setPageBackgroundColor(color: Color) {
        scripts.trySend("setPageBackgroundColor('${color.toHexString()}');")
    }

    fun setLegendTextColor(color: Color) {
        scripts.trySend("setLegendTextColor('${color.toHexString()}');")
    }

    fun connect(chart: IChartApi) {

        // Cache chart
        charts.add(chart)

        // Set initial size for chart
        chart.resize(width = currentSize.width, height = currentSize.height)

        // Send chart js scripts to web engine
        coroutineScope.launch {
            chart.scripts.collect(scripts::trySend)
        }
    }

    fun disconnect(chart: IChartApi) {
        charts.remove(chart)
    }
}
