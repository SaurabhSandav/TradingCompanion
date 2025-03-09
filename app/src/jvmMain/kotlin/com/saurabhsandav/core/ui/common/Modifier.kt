package com.saurabhsandav.core.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
inline fun Modifier.thenIf(
    condition: Boolean,
    noinline ifFalse: @Composable (Modifier.() -> Modifier)? = null,
    ifTrue: @Composable Modifier.() -> Modifier,
): Modifier = when {
    condition -> then(ifTrue(Modifier))
    ifFalse != null -> then(ifFalse(Modifier))
    else -> this
}

@Composable
inline fun <T> Modifier.thenIfNotNull(
    value: T?,
    ifNotNull: @Composable Modifier.(T) -> Modifier,
): Modifier = when {
    value != null -> then(ifNotNull(Modifier, value))
    else -> this
}
