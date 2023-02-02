package com.saurabhsandav.core.ui.common.table

import androidx.compose.runtime.Composable

class Column<T>(
    val header: (@Composable () -> Unit)? = null,
    val span: Float = 1F,
    val content: @Composable (T) -> Unit,
)
