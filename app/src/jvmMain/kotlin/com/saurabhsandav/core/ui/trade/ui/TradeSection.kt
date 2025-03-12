package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun TradeSection(
    title: String,
    subtitle: String,
    trailingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {

    TradeSection(
        modifier = modifier,
        headerContent = {

            // Header
            ListItem(
                headlineContent = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                supportingContent = { Text(subtitle) },
                trailingContent = trailingContent,
                colors = ListItemDefaults.colors(
                    containerColor = TradeSectionDefaults.background,
                ),
            )

            HorizontalDivider()
        },
        content = content,
    )
}

@Composable
internal fun TradeSection(
    headerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {

    Surface(
        modifier = modifier,
        shape = CardDefaults.shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        Column(
            modifier = Modifier.animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // Header
            Column(
                modifier = Modifier.background(TradeSectionDefaults.background),
                content = { headerContent() },
            )

            content()
        }
    }
}

@Composable
internal fun TradeSectionButton(
    onClick: () -> Unit,
    text: String,
    icon: @Composable () -> Unit = {

        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add",
        )
    },
) {

    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        content = {

            Box(
                modifier = Modifier.size(ButtonDefaults.IconSize),
                propagateMinConstraints = true,
                content = { icon() },
            )

            Spacer(Modifier.width(ButtonDefaults.IconSpacing))

            Text(text)
        },
    )
}

object TradeSectionDefaults {

    @get:Composable
    val background: Color
        get() = MaterialTheme.colorScheme.surfaceContainerLow

    @get:Composable
    val backgroundLow: Color
        get() = MaterialTheme.colorScheme.surfaceContainerLowest
}
