package com.saurabhsandav.core.ui.tags.model

import com.saurabhsandav.core.trades.model.TradeTagId

data class TagsState(
    val tags: List<Tag>?,
    val eventSink: (TagsEvent) -> Unit,
) {

    data class Tag(
        val id: TradeTagId,
        val name: String,
        val description: String,
    )
}
