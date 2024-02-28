package com.saurabhsandav.core.ui.common

import androidx.compose.runtime.mutableStateListOf

class SelectionManager<T> {

    private val _selection = mutableStateListOf<T>()
    val selection: List<T>
        get() = _selection

    fun select(item: T) {

        when (item) {
            // Deselect item
            in selection -> _selection.remove(item)

            // Select item
            else -> _selection.add(item)
        }
    }

    fun clear() {
        _selection.clear()
    }
}
