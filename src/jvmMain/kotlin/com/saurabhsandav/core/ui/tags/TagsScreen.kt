package com.saurabhsandav.core.ui.tags

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tags.form.TagFormDialog
import com.saurabhsandav.core.ui.tags.form.TagFormType
import com.saurabhsandav.core.ui.tags.model.TagsState.Tag
import com.saurabhsandav.core.ui.tags.ui.TagListItem
import com.saurabhsandav.core.ui.tradecontent.ProfileTagId
import kotlinx.collections.immutable.ImmutableList

@Composable
fun TagsScreen(
    profileId: ProfileId,
    tags: ImmutableList<Tag>,
    onDeleteTag: (ProfileTagId) -> Unit,
) {

    var showNewTagDialog by state { false }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showNewTagDialog = true }) {
                Text(text = "New Tag")
            }
        },
    ) {

        Box {

            val lazyListState = rememberLazyListState()

            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {

                items(
                    items = tags,
                    key = { it.id },
                ) { tag ->

                    TagListItem(
                        tag = tag,
                        onDelete = { onDeleteTag(tag.id) },
                    )
                }
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(lazyListState)
            )
        }
    }

    if (showNewTagDialog) {

        TagFormDialog(
            profileId = profileId,
            type = TagFormType.New,
            onCloseRequest = { showNewTagDialog = false },
        )
    }
}
