package com.saurabhsandav.core.ui.tags.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.tags.screen.model.TagsScreenEvent.*

internal class TagsLandingSwitcherItem(
    tagsScreenModule: TagsScreenModule,
) : LandingSwitcherItem {

    private val presenter = tagsScreenModule.presenter()

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        TagsScreen(
            tags = state.tags,
            onNewTag = { state.eventSink(NewTag) },
            onNewTagFromExisting = { id -> state.eventSink(NewTagFromExisting(id)) },
            onEditTag = { id -> state.eventSink(EditTag(id)) },
            onDeleteTag = { id -> state.eventSink(DeleteTag(id)) },
        )
    }
}
