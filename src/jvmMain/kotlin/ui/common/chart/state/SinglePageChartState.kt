package ui.common.chart.state

import chart.createChart
import chart.options.ChartOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SinglePageChartState(
    coroutineScope: CoroutineScope,
    options: ChartOptions = ChartOptions(),
) : ChartState(coroutineScope) {

    val chart = createChart(options = options)

    override val jsCommands: Flow<String>
        get() = chart.scripts

    override fun resize(width: Int, height: Int) {
        chart.resize(width = width, height = height)
    }

    override fun onCallback(callbackMessage: String) {

        // To prevent exceptions being swallowed by JS
        coroutineScope.launch {
            chart.onCallback(callbackMessage)
        }
    }
}
