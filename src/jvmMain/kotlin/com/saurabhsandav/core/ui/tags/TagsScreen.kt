package com.saurabhsandav.core.ui.tags

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.tags.model.TagsState.Tag
import com.saurabhsandav.core.ui.tags.ui.TagsList

@Composable
fun TagsScreen(
    tags: List<Tag>?,
    onNewTag: () -> Unit,
    onNewTagFromExisting: (TradeTagId) -> Unit,
    onEditTag: (TradeTagId) -> Unit,
    onDeleteTag: (TradeTagId) -> Unit,
) {

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNewTag) {
                Text(text = "New Tag")
            }
        },
    ) {

        TagsList(
            tags = tags,
            onNewTagFromExisting = onNewTagFromExisting,
            onEditTag = onEditTag,
            onDeleteTag = onDeleteTag,
        )
    }
}
