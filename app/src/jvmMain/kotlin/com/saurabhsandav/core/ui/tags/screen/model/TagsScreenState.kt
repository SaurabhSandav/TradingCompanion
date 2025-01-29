package com.saurabhsandav.core.ui.tags.screen.model

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.trades.model.TradeTagId

data class TagsScreenState(
    val tags: List<Tag>?,
    val eventSink: (TagsScreenEvent) -> Unit,
) {

    data class Tag(
        val id: TradeTagId,
        val name: String,
        val description: String?,
        val color: Color?,
    )
}
