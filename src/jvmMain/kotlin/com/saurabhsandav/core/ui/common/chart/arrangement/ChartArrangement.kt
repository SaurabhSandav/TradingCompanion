package com.saurabhsandav.core.ui.common.chart.arrangement

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow

abstract class ChartArrangement {

    private val _scripts = Channel<String>(Channel.UNLIMITED)
    val scripts = _scripts.consumeAsFlow()

    protected fun executeJs(script: String) {
        _scripts.trySend(script)
    }

    open fun onCallback(message: String): Boolean = false

    companion object
}
