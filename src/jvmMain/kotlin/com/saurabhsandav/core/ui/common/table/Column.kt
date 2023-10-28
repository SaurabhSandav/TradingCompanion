package com.saurabhsandav.core.ui.common.table

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

class Column<T>(
    val header: (@Composable () -> Unit)? = null,
    val width: Width = Width.Weight(1F),
    val content: @Composable (T) -> Unit,
) {

    sealed class Width {

        data class Fixed(val width: Dp) : Width()

        data class Weight(val weight: Float) : Width()
    }
}

context (RowScope)
internal fun Column.Width.asModifier(): Modifier {
    return when (this) {
        is Column.Width.Fixed -> Modifier.width(width)
        is Column.Width.Weight -> Modifier.weight(weight)
    }
}
