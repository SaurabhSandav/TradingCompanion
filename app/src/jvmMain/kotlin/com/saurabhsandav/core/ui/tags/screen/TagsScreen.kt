package com.saurabhsandav.core.ui.tags.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.PrimaryOptionsBar
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.tags.screen.ui.TagsList

@Composable
fun TagsScreen(
    tags: List<TradeTag>?,
    onNewTag: () -> Unit,
    onNewTagFromExisting: (TradeTagId) -> Unit,
    onEditTag: (TradeTagId) -> Unit,
    onDeleteTag: (TradeTagId) -> Unit,
) {

    Scaffold { paddingValues ->

        Column(Modifier.padding(paddingValues)) {

            PrimaryOptionsBar {

                Button(
                    onClick = onNewTag,
                    shape = MaterialTheme.shapes.small,
                    content = { Text("New Tag") },
                )
            }

            HorizontalDivider()

            TagsList(
                tags = tags,
                onNewTagFromExisting = onNewTagFromExisting,
                onEditTag = onEditTag,
                onDeleteTag = onDeleteTag,
            )
        }
    }
}
