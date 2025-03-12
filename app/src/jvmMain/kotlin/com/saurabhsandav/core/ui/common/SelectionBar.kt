package com.saurabhsandav.core.ui.common

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun SelectionBar(
    selectionManager: SelectionManager<*>,
    content: @Composable SelectionBarScope.() -> Unit,
) {

    AnimatedVisibility(
        visible = selectionManager.selection.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut(),
    ) {

        Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {

            Row(
                modifier = Modifier.height(48.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                IconButton(
                    onClick = selectionManager::clear,
                    content = {
                        Icon(Icons.Default.Close, contentDescription = "Clear selection")
                    },
                )

                Spacer(Modifier.width(MaterialTheme.dimens.rowHorizontalSpacing))

                Text(
                    modifier = Modifier.animateContentSize(),
                    text = kotlin.run {
                        val size = selectionManager.selection.size
                        "$size ${if (size == 1) "item" else "items"} selected"
                    },
                )

                Spacer(Modifier.width(MaterialTheme.dimens.rowHorizontalSpacing))

                VerticalDivider()

                SelectionBarScopeImpl.content()
            }
        }
    }
}

interface SelectionBarScope {

    @Composable
    fun Item(
        onClick: () -> Unit,
        text: String,
    )
}

private object SelectionBarScopeImpl : SelectionBarScope {

    @Composable
    override fun Item(
        onClick: () -> Unit,
        text: String,
    ) {

        TextButton(
            modifier = Modifier.fillMaxHeight(),
            onClick = onClick,
            shape = RectangleShape,
            content = { Text(text) },
        )
    }
}
