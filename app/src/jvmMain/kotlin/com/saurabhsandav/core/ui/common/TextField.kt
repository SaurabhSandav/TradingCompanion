package com.saurabhsandav.core.ui.common

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Stable

@Stable
fun InputTransformation.trim(): InputTransformation = this.then(TrimInputTransformation)

private val TrimInputTransformation = InputTransformation {

    val charSequence = asCharSequence()

    val start = charSequence.indexOfFirst { !it.isWhitespace() }
    if (start > 0) delete(0, start)

    val end = charSequence.indexOfLast { !it.isWhitespace() } + 1
    if (end < length) delete(end, length)
}
