package com.saurabhsandav.core.ui.common.table

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal fun <T> LazyTable(
    schema: TableSchema<T>,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit = {

        DefaultTableHeader(schema)

        HorizontalDivider()
    },
    content: TableScope<T>.() -> Unit,
) {

    Column(modifier = modifier) {

        headerContent()

        Box {

            val lazyListState = rememberLazyListState()

            LazyColumn(state = lazyListState) {
                TableScopeImpl(this, schema).content()
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(lazyListState)
            )
        }
    }
}
