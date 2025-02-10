package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.stockchart.StockChart

@Composable
fun StockChartTopBar(
    stockChart: StockChart,
    onOpenTickerSelection: () -> Unit,
    onOpenTimeframeSelection: () -> Unit,
) {

    Row(
        modifier = Modifier.height(48.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        TextButton(
            onClick = onOpenTickerSelection,
            icon = {

                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            text = { Text(stockChart.params.ticker) },
        )

        VerticalDivider()

        TextButton(
            onClick = onOpenTimeframeSelection,
            icon = {

                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            text = { Text(stockChart.params.timeframe.toLabel()) },
        )

        VerticalDivider()
    }
}

@Composable
private fun TextButton(
    onClick: () -> Unit,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable () -> Unit,
) {

    TextButton(
        modifier = Modifier.fillMaxHeight(),
        onClick = onClick,
        shape = RectangleShape,
        contentPadding = when {
            icon == null -> ButtonDefaults.TextButtonContentPadding
            else -> ButtonDefaults.TextButtonWithIconContentPadding
        },
    ) {

        if (icon != null) {

            icon()

            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }

        text()
    }
}
