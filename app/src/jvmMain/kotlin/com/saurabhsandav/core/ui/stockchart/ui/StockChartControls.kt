package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun StockChartControls(
    stockChart: StockChart,
    customControls: @Composable ColumnScope.(StockChart) -> Unit,
) {

    var isCollapsed by state { false }

    CollapsiblePane(
        isCollapsed = isCollapsed,
        onExpandRequest = { isCollapsed = false },
    ) {

        Column(
            modifier = Modifier.width(250.dp)
                .fillMaxHeight()
                .padding(MaterialTheme.dimens.containerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(
                space = MaterialTheme.dimens.columnVerticalSpacing,
                alignment = Alignment.CenterVertically,
            ),
        ) {

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { isCollapsed = true },
                content = { Text("Hide Pane") },
            )

            customControls(stockChart)
        }
    }
}

@Composable
private fun CollapsiblePane(
    isCollapsed: Boolean,
    onExpandRequest: () -> Unit,
    content: @Composable () -> Unit,
) {

    AnimatedContent(
        targetState = isCollapsed,
    ) { isCollapsedAC ->

        when {
            isCollapsedAC -> IconButtonWithTooltip(
                modifier = Modifier.width(56.dp).fillMaxHeight().clickable(onClick = onExpandRequest),
                onClick = onExpandRequest,
                tooltipText = "Open controls",
                content = {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Open controls")
                },
            )

            else -> content()
        }
    }
}
