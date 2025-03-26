package com.saurabhsandav.core.ui.common

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun BoxWithScrollbar(
    scrollbarAdapter: ScrollbarAdapter?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {

    Box(
        modifier = modifier,
        propagateMinConstraints = true,
    ) {

        content()

        if (scrollbarAdapter != null) {

            VerticalScrollbar(
                modifier = Modifier.fillMaxSize().wrapContentWidth(Alignment.End),
                adapter = scrollbarAdapter,
            )
        }
    }
}
