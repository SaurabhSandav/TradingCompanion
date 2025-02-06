package com.saurabhsandav.core.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.onTextFieldClickOrEnter(
    block: () -> Unit,
): Modifier = pointerInput(block) {
    awaitEachGesture {
        // Must be PointerEventPass.Initial to observe events before the text field consumes them
        // in the Main pass
        awaitFirstDown(pass = PointerEventPass.Initial)
        val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
        if (upEvent != null) block()
    }
}.onPreviewKeyEvent { keyEvent ->

    when {
        keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown -> {
            block()
            true
        }

        else -> false
    }
}
