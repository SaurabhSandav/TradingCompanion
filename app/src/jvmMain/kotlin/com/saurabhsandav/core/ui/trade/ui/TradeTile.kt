package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun TradeTile(
    title: String,
    value: @Composable () -> Unit,
) {

    OutlinedCard {

        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.containerPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // Value
            ProvideTextStyle(MaterialTheme.typography.titleLarge, value)

            // Title
            Text(text = title)
        }
    }
}

@Composable
internal fun TradeTileContainer(
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit,
) {

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.dimens.rowHorizontalSpacing,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowVerticalSpacing),
        content = content,
    )
}
