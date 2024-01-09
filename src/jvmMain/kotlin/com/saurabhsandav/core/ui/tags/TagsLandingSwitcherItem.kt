package com.saurabhsandav.core.ui.tags

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.tags.model.TagsEvent.DeleteTag

internal class TagsLandingSwitcherItem(
    private val tagsModule: TagsModule,
) : LandingSwitcherItem {

    private val presenter = tagsModule.presenter()

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        TagsScreen(
            profileId = tagsModule.profileId,
            tags = state.tags,
            onDeleteTag = { id -> state.eventSink(DeleteTag(id)) },
        )
    }
}
