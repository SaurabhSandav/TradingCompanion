package ui.common

import androidx.compose.runtime.*

@Composable
internal inline fun <T> state(value: @DisallowComposableCalls () -> T): MutableState<T> {
    return remember { mutableStateOf(value()) }
}

@Composable
internal inline fun <T> state(
    key1: Any?,
    value: @DisallowComposableCalls () -> T,
): MutableState<T> {
    return remember(key1) { mutableStateOf(value()) }
}
