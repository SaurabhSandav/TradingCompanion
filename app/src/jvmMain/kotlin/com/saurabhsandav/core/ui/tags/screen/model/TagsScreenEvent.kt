package com.saurabhsandav.core.ui.tags.screen.model

import com.saurabhsandav.core.trades.model.TradeTagId

sealed class TagsScreenEvent {

    data class DeleteTag(
        val id: TradeTagId,
    ) : TagsScreenEvent()
}
