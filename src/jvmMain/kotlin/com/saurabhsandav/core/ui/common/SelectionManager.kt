package com.saurabhsandav.core.ui.common

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot

@Stable
class SelectionManager<T> {

    private val _selection = mutableStateListOf<T>()
    val selection: List<T>
        get() = _selection

    private var _inMultiSelectMode by mutableStateOf(false)
    val inMultiSelectMode: Boolean
        get() = _inMultiSelectMode

    fun select(
        item: T,
        ifSingleSelect: (T) -> Unit = {},
    ) = Snapshot.withMutableSnapshot {

        when {
            inMultiSelectMode -> toggleSelection(item)
            else -> ifSingleSelect(item)
        }
    }

    fun multiSelect(item: T) = Snapshot.withMutableSnapshot {

        _inMultiSelectMode = true

        toggleSelection(item)
    }

    fun clear() = Snapshot.withMutableSnapshot {
        _selection.clear()
        _inMultiSelectMode = false
    }

    private fun toggleSelection(item: T) {

        when (item) {
            // Deselect item
            in selection -> {

                _selection.remove(item)

                _inMultiSelectMode = _selection.isNotEmpty()
            }

            // Select item
            else -> _selection.add(item)
        }
    }
}
