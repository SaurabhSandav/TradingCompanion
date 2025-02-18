package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.saurabhsandav.core.ui.stockchart.ui.ChartsLayout.GridRect
import com.saurabhsandav.core.ui.stockchart.ui.ChartsLayout.GridSize
import kotlin.math.floor

sealed interface ChartsLayout {

    val gridSize: GridSize

    val rects: List<GridRect>

    data class GridSize(
        val columns: Int,
        val rows: Int,
    )

    data class GridRect(
        val left: Int,
        val top: Int,
        val columns: Int,
        val rows: Int,
    )
}

fun ChartsLayout.getComposeRects(size: Size): List<Rect> {

    val widthPerColumn = floor(size.width / gridSize.columns)
    val heightPerRow = floor(size.height / gridSize.rows)

    val widthExtra = size.width % gridSize.columns
    val heightExtra = size.height % gridSize.rows

    return rects.map { rect ->

        val consumeExtraWidth = rect.left + rect.columns == gridSize.columns
        val consumeExtraHeight = rect.top + rect.rows == gridSize.rows

        Rect(
            offset = Offset(
                x = widthPerColumn * rect.left,
                y = heightPerRow * rect.top,
            ),
            size = Size(
                width = widthPerColumn * rect.columns + if (consumeExtraWidth) widthExtra else 0F,
                height = heightPerRow * rect.rows + if (consumeExtraHeight) heightExtra else 0F,
            ),
        )
    }
}

object Tabs : ChartsLayout {

    override val gridSize = GridSize(columns = 1, rows = 1)

    override val rects = listOf(
        GridRect(left = 0, top = 0, columns = 1, rows = 1),
    )
}

sealed interface PanesLayout : ChartsLayout

sealed interface TwoPanes : PanesLayout {

    object ColumnsOnly : TwoPanes {

        override val gridSize = GridSize(columns = 2, rows = 1)

        override val rects = listOf(
            GridRect(left = 0, top = 0, columns = 1, rows = 1),
            GridRect(left = 1, top = 0, columns = 1, rows = 1),
        )
    }

    object RowsOnly : TwoPanes {

        override val gridSize = GridSize(columns = 1, rows = 2)

        override val rects = listOf(
            GridRect(left = 0, top = 0, columns = 1, rows = 1),
            GridRect(left = 0, top = 1, columns = 1, rows = 1),
        )
    }
}

sealed interface ThreePanes : PanesLayout {

    object ColumnsOnly : ThreePanes {

        override val gridSize = GridSize(columns = 3, rows = 1)

        override val rects = listOf(
            GridRect(left = 0, top = 0, columns = 1, rows = 1),
            GridRect(left = 1, top = 0, columns = 1, rows = 1),
            GridRect(left = 2, top = 0, columns = 1, rows = 1),
        )
    }

    object RowsOnly : ThreePanes {

        override val gridSize = GridSize(columns = 1, rows = 3)

        override val rects = listOf(
            GridRect(left = 0, top = 0, columns = 1, rows = 1),
            GridRect(left = 0, top = 1, columns = 1, rows = 1),
            GridRect(left = 0, top = 2, columns = 1, rows = 1),
        )
    }

    object Exotic1 : ThreePanes {

        override val gridSize = GridSize(columns = 2, rows = 2)

        override val rects = listOf(
            GridRect(left = 0, top = 0, columns = 1, rows = 2),
            GridRect(left = 1, top = 0, columns = 1, rows = 1),
            GridRect(left = 1, top = 1, columns = 1, rows = 1),
        )
    }

    object Exotic2 : ThreePanes {

        override val gridSize = GridSize(columns = 2, rows = 2)

        override val rects = listOf(
            GridRect(left = 0, top = 0, columns = 1, rows = 1),
            GridRect(left = 1, top = 0, columns = 1, rows = 1),
            GridRect(left = 0, top = 1, columns = 2, rows = 1),
        )
    }

    object Exotic3 : ThreePanes {

        override val gridSize = GridSize(columns = 2, rows = 2)

        override val rects = listOf(
            GridRect(left = 0, top = 0, columns = 2, rows = 1),
            GridRect(left = 0, top = 1, columns = 1, rows = 1),
            GridRect(left = 1, top = 1, columns = 1, rows = 1),
        )
    }

    object Exotic4 : ThreePanes {

        override val gridSize = GridSize(columns = 2, rows = 2)

        override val rects = listOf(
            GridRect(left = 0, top = 0, columns = 1, rows = 1),
            GridRect(left = 0, top = 1, columns = 1, rows = 1),
            GridRect(left = 1, top = 0, columns = 1, rows = 2),
        )
    }
}

sealed interface FourPanes : PanesLayout {

    object Equal : FourPanes {

        override val gridSize = GridSize(columns = 2, rows = 2)

        override val rects = listOf(
            GridRect(left = 0, top = 0, columns = 1, rows = 1),
            GridRect(left = 1, top = 0, columns = 1, rows = 1),
            GridRect(left = 0, top = 1, columns = 1, rows = 1),
            GridRect(left = 1, top = 1, columns = 1, rows = 1),
        )
    }

    object ColumnsOnly : FourPanes {

        override val gridSize = GridSize(columns = 4, rows = 1)

        override val rects = listOf(
            GridRect(left = 0, top = 0, columns = 1, rows = 1),
            GridRect(left = 1, top = 0, columns = 1, rows = 1),
            GridRect(left = 2, top = 0, columns = 1, rows = 1),
            GridRect(left = 3, top = 0, columns = 1, rows = 1),
        )
    }

    object RowsOnly : FourPanes {

        override val gridSize = GridSize(columns = 1, rows = 4)

        override val rects = listOf(
            GridRect(left = 0, top = 0, columns = 1, rows = 1),
            GridRect(left = 0, top = 1, columns = 1, rows = 1),
            GridRect(left = 0, top = 2, columns = 1, rows = 1),
            GridRect(left = 0, top = 3, columns = 1, rows = 1),
        )
    }

    object Exotic1 : FourPanes {

        override val gridSize = GridSize(columns = 2, rows = 3)

        override val rects = listOf(
            GridRect(left = 0, top = 0, columns = 1, rows = 3),
            GridRect(left = 1, top = 0, columns = 1, rows = 1),
            GridRect(left = 1, top = 1, columns = 1, rows = 1),
            GridRect(left = 1, top = 2, columns = 1, rows = 1),
        )
    }

    object Exotic2 : FourPanes {

        override val gridSize = GridSize(columns = 2, rows = 3)

        override val rects = listOf(
            GridRect(left = 0, top = 0, columns = 1, rows = 1),
            GridRect(left = 0, top = 1, columns = 1, rows = 1),
            GridRect(left = 0, top = 2, columns = 1, rows = 1),
            GridRect(left = 1, top = 0, columns = 1, rows = 3),
        )
    }

    object Exotic3 : FourPanes {

        override val gridSize = GridSize(columns = 3, rows = 2)

        override val rects = listOf(
            GridRect(left = 0, top = 0, columns = 3, rows = 1),
            GridRect(left = 0, top = 1, columns = 1, rows = 1),
            GridRect(left = 1, top = 1, columns = 1, rows = 1),
            GridRect(left = 2, top = 1, columns = 1, rows = 1),
        )
    }
}
