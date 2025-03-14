package com.saurabhsandav.core.ui.common.table

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface RowBuilder {

    fun addCell(cell: TableCell, content: @Composable () -> Unit)
}

class RowBuilderImpl : RowBuilder {

    val contents = mutableMapOf<TableCell, @Composable () -> Unit>()

    override fun addCell(cell: TableCell, content: @Composable () -> Unit) {
        contents[cell] = content
    }
}

context(rowBuilder: RowBuilder)
fun TableCell.content(content: @Composable () -> Unit) = rowBuilder.addCell(this, content)

context(rowBuilder: RowBuilder)
inline fun TableCell.text(crossinline text: () -> String) {

    content {

        Text(
            text = text(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
