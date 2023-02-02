package com.saurabhsandav.core.ui.common

import androidx.compose.runtime.*

@Composable
internal inline fun <T> state(crossinline value: @DisallowComposableCalls () -> T): MutableState<T> {
    return remember { mutableStateOf(value()) }
}

@Composable
internal inline fun <T> state(
    key1: Any?,
    crossinline value: @DisallowComposableCalls () -> T,
): MutableState<T> {
    return remember(key1) { mutableStateOf(value()) }
}
