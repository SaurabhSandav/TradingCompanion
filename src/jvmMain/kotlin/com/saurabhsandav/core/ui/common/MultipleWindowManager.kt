package com.saurabhsandav.core.ui.common

import androidx.compose.runtime.mutableStateListOf

internal class MultipleWindowManager<T> {

    private val _windows = mutableStateListOf<WindowEntry<T>>()
    val windows: List<WindowEntry<T>>
        get() = _windows

    fun openNewWindow(params: T) {

        _windows += WindowEntry(
            onCloseRequest = _windows::remove,
            params = params,
        )
    }

    fun closeAll() {
        _windows.clear()
    }

    class WindowEntry<T>(
        val onCloseRequest: (WindowEntry<T>) -> Unit,
        val params: T,
    ) {
        fun close() = onCloseRequest(this)
    }
}
