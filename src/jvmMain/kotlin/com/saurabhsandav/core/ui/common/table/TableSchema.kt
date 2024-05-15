package com.saurabhsandav.core.ui.common.table

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Fixed
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Weight

open class TableSchema {

    private val _cells = mutableListOf<TableCell>()
    val cells: List<TableCell> = _cells

    protected fun cell(
        width: TableCell.Width = Weight(1F),
    ): TableCell {

        val cell = TableCell(width = width)

        _cells.add(cell)

        return cell
    }
}

class TableCell(
    val width: Width,
) {

    sealed class Width {

        data class Fixed(val width: Dp) : Width()

        data class Weight(val weight: Float) : Width()
    }
}

context (RowScope)
internal fun TableCell.Width.asModifier(): Modifier {
    return when (this) {
        is Fixed -> Modifier.width(width)
        is Weight -> Modifier.weight(weight)
    }
}
