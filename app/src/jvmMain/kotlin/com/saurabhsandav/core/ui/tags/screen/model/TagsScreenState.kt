package com.saurabhsandav.core.ui.tags.screen.model

import com.saurabhsandav.core.ui.tags.model.TradeTag

data class TagsScreenState(
    val tags: List<TradeTag>?,
    val eventSink: (TagsScreenEvent) -> Unit,
)
