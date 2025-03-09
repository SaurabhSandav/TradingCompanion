package com.saurabhsandav.core.ui.tags.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.tags.screen.model.TagsScreenEvent.DeleteTag

internal class TagsLandingSwitcherItem(
    private val tagsScreenModule: TagsScreenModule,
) : LandingSwitcherItem {

    private val presenter = tagsScreenModule.presenter()

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        TagsScreen(
            profileId = tagsScreenModule.profileId,
            tags = state.tags,
            onDeleteTag = { id -> state.eventSink(DeleteTag(id)) },
        )
    }
}
