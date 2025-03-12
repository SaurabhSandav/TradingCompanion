package com.saurabhsandav.core.ui.tags.screen.model

import com.saurabhsandav.core.trades.model.TradeTagId

sealed class TagsScreenEvent {

    data object NewTag : TagsScreenEvent()

    data class NewTagFromExisting(
        val id: TradeTagId,
    ) : TagsScreenEvent()

    data class EditTag(
        val id: TradeTagId,
    ) : TagsScreenEvent()

    data class DeleteTag(
        val id: TradeTagId,
    ) : TagsScreenEvent()
}
