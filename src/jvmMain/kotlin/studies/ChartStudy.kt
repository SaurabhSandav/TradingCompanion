package studies

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import chart.IChartApi
import kotlinx.coroutines.CoroutineScope
import ui.common.ResizableChart

internal abstract class ChartStudy : Study {

    @Composable
    final override fun render() {

        render {

            val coroutineScope = rememberCoroutineScope()

            ResizableChart { configure(coroutineScope) }
        }
    }

    @Composable
    protected open fun render(chart: @Composable () -> Unit) {
        chart()
    }

    protected abstract fun IChartApi.configure(scope: CoroutineScope)
}
