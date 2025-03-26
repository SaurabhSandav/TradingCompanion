package com.saurabhsandav.core.ui.common.table

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.BoxWithScrollbar

@Composable
internal fun LazyTable(
    modifier: Modifier = Modifier,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {

    Column(modifier = modifier) {

        headerContent?.invoke(this)

        val lazyListState = rememberLazyListState()

        BoxWithScrollbar(
            scrollbarAdapter = rememberScrollbarAdapter(lazyListState),
        ) {

            LazyColumn(
                state = lazyListState,
                content = content,
            )
        }
    }
}
