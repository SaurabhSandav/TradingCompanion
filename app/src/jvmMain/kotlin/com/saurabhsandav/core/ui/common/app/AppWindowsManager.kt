package com.saurabhsandav.core.ui.common.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf

internal class AppWindowsManager<T> {

    private val _windows = mutableStateListOf<Window<T>>()
    val windows: List<Window<T>> get() = _windows

    @Composable
    fun Windows(content: @Composable (Window<T>) -> Unit) {

        windows.forEach { window ->

            key(window) {

                window.owner.Window {

                    content(window)
                }
            }
        }
    }

    fun newWindow(params: T) {

        _windows += Window(
            params = params,
            onCloseRequest = _windows::remove,
        )
    }

    fun closeAll() {
        _windows.clear()
    }

    class Window<T>(
        val params: T,
        val onCloseRequest: (Window<T>) -> Unit,
        val owner: AppWindowOwner = AppWindowOwner(),
    ) {

        fun toFront() = owner.childrenToFront()

        fun close() = onCloseRequest(this)
    }
}
