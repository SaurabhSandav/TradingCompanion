package com.saurabhsandav.core.ui.common.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppWindowManager(
    initialIsOpen: Boolean = false,
) {

    val owner = AppWindowOwner()

    private var isOpen by mutableStateOf(initialIsOpen)

    @Composable
    fun Window(content: @Composable () -> Unit) {
        if (isOpen) owner.Window(content)
    }

    fun openWindow(toFront: Boolean = true) {
        isOpen = true
        if (toFront) owner.childrenToFront()
    }

    fun closeWindow() {
        isOpen = false
    }
}
