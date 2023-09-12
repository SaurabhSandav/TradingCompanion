package com.saurabhsandav.core.ui.common

import androidx.compose.runtime.*

@Composable
internal inline fun <T> state(crossinline value: @DisallowComposableCalls () -> T): MutableState<T> {
    return remember { mutableStateOf(value()) }
}

@Composable
internal inline fun intState(crossinline value: @DisallowComposableCalls () -> Int): MutableIntState {
    return remember { mutableIntStateOf(value()) }
}

@Composable
internal inline fun <T> state(
    key1: Any?,
    crossinline value: @DisallowComposableCalls () -> T,
): MutableState<T> {
    return remember(key1) { mutableStateOf(value()) }
}

@Composable
internal fun <T> derivedState(calculation: () -> T): State<T> = remember { derivedStateOf(calculation) }
