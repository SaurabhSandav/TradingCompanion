package com.saurabhsandav.core.ui.common

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
internal inline fun <T> state(crossinline value: @DisallowComposableCalls () -> T): MutableState<T> {
    return remember { mutableStateOf(value()) }
}

@Composable
internal inline fun intState(crossinline value: @DisallowComposableCalls () -> Int): MutableIntState {
    return remember { mutableIntStateOf(value()) }
}

@Composable
internal inline fun <T> saveableState(
    vararg inputs: Any?,
    stateSaver: Saver<T, out Any>,
    key: String? = null,
    crossinline init: @DisallowComposableCalls () -> T,
): MutableState<T> {
    return rememberSaveable(
        inputs = inputs,
        stateSaver = stateSaver,
        key = key,
        init = { mutableStateOf(init()) },
    )
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
