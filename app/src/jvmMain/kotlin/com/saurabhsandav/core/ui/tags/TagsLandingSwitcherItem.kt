package com.saurabhsandav.core.ui.tags

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.tags.model.TagsEvent.*

internal class TagsLandingSwitcherItem(
    tagsModule: TagsModule,
) : LandingSwitcherItem {

    private val presenter = tagsModule.presenter()

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
