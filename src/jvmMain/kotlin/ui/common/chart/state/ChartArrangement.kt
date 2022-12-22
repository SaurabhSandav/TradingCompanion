package ui.common.chart.state

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow

abstract class ChartArrangement {

    private val _scripts = Channel<String>(Channel.UNLIMITED)
    val scripts = _scripts.consumeAsFlow()

    protected fun executeJs(script: String) {
        _scripts.trySend(script)
    }

    @JvmInline
    value class ChartContainer(val value: String)

    companion object
}
