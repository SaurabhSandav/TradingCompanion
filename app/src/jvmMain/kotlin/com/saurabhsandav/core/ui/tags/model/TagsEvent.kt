package com.saurabhsandav.core.ui.tags.model

import com.saurabhsandav.core.trades.model.TradeTagId

sealed class TagsEvent {

    data object NewTag : TagsEvent()

    data class NewTagFromExisting(val id: TradeTagId) : TagsEvent()

    data class EditTag(val id: TradeTagId) : TagsEvent()

    data class DeleteTag(val id: TradeTagId) : TagsEvent()
}
