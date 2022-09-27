package table

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntOffset
import utils.state

interface TableRowScope : LazyItemScope

internal class TableRowScopeImpl(
    private val lazyItemScope: LazyItemScope,
) : TableRowScope {

    @ExperimentalFoundationApi
    override fun Modifier.animateItemPlacement(animationSpec: FiniteAnimationSpec<IntOffset>): Modifier {
        return with(lazyItemScope) { animateItemPlacement(animationSpec) }
    }

    override fun Modifier.fillParentMaxHeight(fraction: Float): Modifier {
        return with(lazyItemScope) { fillParentMaxHeight(fraction) }
    }

    override fun Modifier.fillParentMaxSize(fraction: Float): Modifier {
        return with(lazyItemScope) { fillParentMaxSize(fraction) }
    }

    override fun Modifier.fillParentMaxWidth(fraction: Float): Modifier {
        return with(lazyItemScope) { fillParentMaxWidth(fraction) }
    }
}

@Composable
fun <T> DefaultTableRow(
    item: T,
    schema: TableSchema<T>,
    modifier: Modifier = Modifier,
) {

    var rowActive by state { false }

    Row(
        modifier = Modifier
            .background(color = if (rowActive) Color.LightGray else Color.White)
            .onPointerEvent(PointerEventType.Enter) { rowActive = true }
            .onPointerEvent(PointerEventType.Exit) { rowActive = false }
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        schema.columns.forEach { column ->
            Box(Modifier.weight(1F)) {
                column.content(item)
            }
        }
    }
}
