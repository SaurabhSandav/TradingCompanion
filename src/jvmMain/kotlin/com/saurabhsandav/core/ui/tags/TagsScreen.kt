package com.saurabhsandav.core.ui.tags

import androidx.compose.material3.Scaffold
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

    Scaffold {

        TagsList(
            tags = tags,
            onNewTag = onNewTag,
            onNewTagFromExisting = onNewTagFromExisting,
            onEditTag = onEditTag,
            onDeleteTag = onDeleteTag,
        )
    }
}
