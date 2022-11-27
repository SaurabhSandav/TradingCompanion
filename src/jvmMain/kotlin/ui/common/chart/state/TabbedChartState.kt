package ui.common.chart.state

import chart.IChartApi
import chart.createChart
import chart.options.ChartOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class TabbedChartState(
    coroutineScope: CoroutineScope,
) : ChartState(coroutineScope) {

    private val tabs = mutableListOf<Tab>()

    private val _jsCommands = MutableSharedFlow<String>(
        replay = Int.MAX_VALUE,
        extraBufferCapacity = Int.MAX_VALUE,
    )
    override val jsCommands: Flow<String> = _jsCommands.asSharedFlow()

    override fun resize(width: Int, height: Int) {
        // Resize all charts
        tabs.forEach { it.chart.resize(width = width, height = height) }
    }

    override fun onCallback(callbackMessage: String) {

        // To prevent exceptions being swallowed by JS
        coroutineScope.launch {
            tabs.forEach { it.chart.onCallback(callbackMessage) }
        }
    }

    fun addChart(name: String, options: ChartOptions = ChartOptions()): IChartApi {

        // Error if chart name already exists
        check(!tabs.any { it.chart.name == name })

        // Create hidden div for new chart
        _jsCommands.tryEmit(
            """|
            |(function() {
            |  var iDiv = document.createElement('div');
            |  iDiv.id = '$name';
            |  iDiv.className = 'tabcontent';
            |  iDiv.style.display = "none";
            |  document.body.appendChild(iDiv);
            |})()
            """.trimMargin()
        )

        // Create chart inside previously created div
        val chart = createChart("document.getElementById('$name')", options, name)
        val scope = CoroutineScope(coroutineScope.coroutineContext + Job())

        // Add to tabs
        tabs += Tab(chart, scope)

        // Collect chart js commands
        scope.launch {
            chart.scripts.collect(_jsCommands::emit)
        }

        return chart
    }

    fun removeChart(chart: IChartApi) {

        // Remove chart elements
        chart.remove()

        // Delete chart div
        _jsCommands.tryEmit("document.getElementById('${chart.name}').remove();")

        // Remove ChartInstance from map
        _jsCommands.tryEmit("charts.delete(\"${chart.name}\")")

        // Find chart tab
        val tab = tabs.find { it.chart == chart }

        // Remove tab
        tabs.remove(tab)

        // Stop collecting chart js commands
        tab?.scope?.cancel()
    }

    fun showChart(chart: IChartApi) {

        // Hide all chart divs, then show selected chart div
        _jsCommands.tryEmit(
            """|
            |(function() {
            |  var tabcontent = document.getElementsByClassName("tabcontent");
            |  for (i = 0; i < tabcontent.length; i++) {
            |    tabcontent[i].style.display = "none";
            |  }
            |  
            |  document.getElementById('${chart.name}').style.display = "block";
            |})()
            """.trimMargin()
        )
    }

    private class Tab(
        val chart: IChartApi,
        val scope: CoroutineScope,
    )
}
