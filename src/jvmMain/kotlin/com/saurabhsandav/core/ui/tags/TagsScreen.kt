package com.saurabhsandav.core.ui.tags

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tags.form.TagFormDialog
import com.saurabhsandav.core.ui.tags.form.TagFormType
import com.saurabhsandav.core.ui.tags.model.TagsEvent.DeleteTag
import com.saurabhsandav.core.ui.tags.model.TagsState.Tag
import com.saurabhsandav.core.ui.tags.ui.TagListItem
import com.saurabhsandav.core.ui.tradecontent.ProfileTagId
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TagsWindow(
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { appModule.tagsModule(scope).presenter() }
    val state by presenter.state.collectAsState()

    AppWindow(
        title = "Tags",
        onCloseRequest = onCloseRequest,
    ) {

        Box(Modifier.wrapContentSize()) {

            when (val profileId = state.profileId) {
                null -> CircularProgressIndicator()
                else -> TagsScreen(
                    profileId = profileId,
                    tags = state.tags,
                    onDeleteTag = { id -> state.eventSink(DeleteTag(id)) },
                )
            }
        }
    }
}

@Composable
private fun TagsScreen(
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

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier.verticalScroll(scrollState).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {

                tags.forEach { tag ->

                    key(tag.id) {

                        TagListItem(
                            tag = tag,
                            onDelete = { onDeleteTag(tag.id) },
                        )
                    }
                }
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState)
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
