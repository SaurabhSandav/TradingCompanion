package ui.common.table

import androidx.compose.runtime.Composable

class Column<T>(
    val header: (@Composable () -> Unit)? = null,
    val content: @Composable (T) -> Unit,
)
