package com.saurabhsandav.core.ui.common

import androidx.compose.animation.*
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.Ref

@Composable
inline fun <T> AnimatedVisibilityForNullable(
    value: T?,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = shrinkOut() + fadeOut(),
    label: String = "AnimatedVisibility",
    crossinline content: @Composable AnimatedVisibilityScope.(T) -> Unit,
) {

    val ref = remember { Ref<T>() }

    ref.value = value ?: ref.value

    AnimatedVisibility(
        modifier = modifier,
        visible = value != null,
        enter = enter,
        exit = exit,
        label = label,
        content = {
            ref.value?.let { value ->
                content(value)
            }
        }
    )
}

@Suppress("UnusedReceiverParameter")
@Composable
inline fun <T> RowScope.AnimatedVisibilityForNullable(
    value: T?,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandHorizontally(),
    exit: ExitTransition = fadeOut() + shrinkHorizontally(),
    label: String = "AnimatedVisibility",
    crossinline content: @Composable AnimatedVisibilityScope.(T) -> Unit,
) {
    com.saurabhsandav.core.ui.common.AnimatedVisibilityForNullable(
        value = value,
        modifier = modifier,
        enter = enter,
        exit = exit,
        label = label,
        content = content,
    )
}

@Suppress("UnusedReceiverParameter")
@Composable
inline fun <T> ColumnScope.AnimatedVisibilityForNullable(
    value: T?,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandVertically(),
    exit: ExitTransition = fadeOut() + shrinkVertically(),
    label: String = "AnimatedVisibility",
    crossinline content: @Composable AnimatedVisibilityScope.(T) -> Unit,
) {
    com.saurabhsandav.core.ui.common.AnimatedVisibilityForNullable(
        value = value,
        modifier = modifier,
        enter = enter,
        exit = exit,
        label = label,
        content = content,
    )
}
