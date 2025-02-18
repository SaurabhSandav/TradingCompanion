package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.app.AppDialog
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun Panes2SelectionDialog(
    onDismissRequest: () -> Unit,
    onSetLayout: (PanesLayout) -> Unit,
) {

    PanePreviewsContainer(
        onDismissRequest = onDismissRequest,
    ) {

        PanePreview(
            layout = TwoPanes.ColumnsOnly,
            onSetLayout = onSetLayout,
        )

        PanePreview(
            layout = TwoPanes.RowsOnly,
            onSetLayout = onSetLayout,
        )
    }
}

@Composable
internal fun Panes3SelectionDialog(
    onDismissRequest: () -> Unit,
    onSetLayout: (PanesLayout) -> Unit,
) {

    PanePreviewsContainer(
        onDismissRequest = onDismissRequest,
    ) {

        PanePreview(
            layout = ThreePanes.ColumnsOnly,
            onSetLayout = onSetLayout,
        )

        PanePreview(
            layout = ThreePanes.RowsOnly,
            onSetLayout = onSetLayout,
        )

        PanePreview(
            layout = ThreePanes.Exotic1,
            onSetLayout = onSetLayout,
        )

        PanePreview(
            layout = ThreePanes.Exotic2,
            onSetLayout = onSetLayout,
        )

        PanePreview(
            layout = ThreePanes.Exotic3,
            onSetLayout = onSetLayout,
        )

        PanePreview(
            layout = ThreePanes.Exotic4,
            onSetLayout = onSetLayout,
        )
    }
}

@Composable
internal fun Panes4SelectionDialog(
    onDismissRequest: () -> Unit,
    onSetLayout: (PanesLayout) -> Unit,
) {

    PanePreviewsContainer(
        onDismissRequest = onDismissRequest,
    ) {

        PanePreview(
            layout = FourPanes.Equal,
            onSetLayout = onSetLayout,
        )

        PanePreview(
            layout = FourPanes.ColumnsOnly,
            onSetLayout = onSetLayout,
        )

        PanePreview(
            layout = FourPanes.RowsOnly,
            onSetLayout = onSetLayout,
        )

        PanePreview(
            layout = FourPanes.Exotic1,
            onSetLayout = onSetLayout,
        )

        PanePreview(
            layout = FourPanes.Exotic2,
            onSetLayout = onSetLayout,
        )

        PanePreview(
            layout = FourPanes.Exotic3,
            onSetLayout = onSetLayout,
        )
    }
}

@Composable
private fun PanePreviewsContainer(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {

    AppDialog(
        onDismissRequest = onDismissRequest,
    ) {

        FlowRow(
            modifier = Modifier.padding(MaterialTheme.dimens.containerPadding),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
            content = { content() },
        )
    }
}

@Composable
private fun PanePreview(
    onSetLayout: (PanesLayout) -> Unit,
    layout: PanesLayout,
) {

    Canvas(
        modifier = Modifier.size(64.dp)
            .clickable { onSetLayout(layout) }
            .padding(8.dp),
    ) {

        // Border
        drawRect(Color.White, style = Stroke())

        // Panes
        layout.getComposeRects(size).forEach { rect ->

            drawRect(
                color = Color.White,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(),
            )
        }
    }
}
