package com.saurabhsandav.core.ui.tags.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trades.model.TradeTagId
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class TagsState(
    val tags: ImmutableList<Tag>,
    val eventSink: (TagsEvent) -> Unit,
) {

    @Immutable
    data class Tag(
        val id: TradeTagId,
        val name: String,
        val description: String,
    )
}
