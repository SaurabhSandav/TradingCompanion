package com.saurabhsandav.core.ui.common.table

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface RowBuilder {

    operator fun TableCell.invoke(content: @Composable () -> Unit)
}

class RowBuilderImpl : RowBuilder {

    val contents = mutableMapOf<TableCell, @Composable () -> Unit>()

    override operator fun TableCell.invoke(content: @Composable () -> Unit) {
        contents[this] = content
    }
}

context(RowBuilder)
inline fun TableCell.text(crossinline text: () -> String) {
    invoke {

        Text(
            text = text(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
