package com.saurabhsandav.core.ui.tags.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.tradecontent.ProfileTagId
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class TagsState(
    val tags: ImmutableList<Tag>,
    val eventSink: (TagsEvent) -> Unit,
) {

    @Immutable
    data class Tag(
        val id: ProfileTagId,
        val name: String,
        val description: String,
    )
}
