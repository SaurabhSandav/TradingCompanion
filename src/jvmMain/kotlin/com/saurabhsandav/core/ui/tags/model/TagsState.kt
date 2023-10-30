package com.saurabhsandav.core.ui.tags.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeTagId
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TagsState(
    val profileId: ProfileId?,
    val tags: ImmutableList<Tag>,
    val eventSink: (TagsEvent) -> Unit,
) {

    @Immutable
    data class Tag(
        val id: ProfileTagId,
        val name: String,
        val description: String,
    )

    @Immutable
    data class ProfileTagId(
        val profileId: ProfileId,
        val tagId: TradeTagId,
    )
}
