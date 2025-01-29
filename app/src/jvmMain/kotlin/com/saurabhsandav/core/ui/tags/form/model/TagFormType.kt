package com.saurabhsandav.core.ui.tags.form.model

import com.saurabhsandav.core.trades.model.TradeTagId

sealed class TagFormType {

    data object New : TagFormType()

    data class NewFromExisting(val id: TradeTagId) : TagFormType()

    data class Edit(val id: TradeTagId) : TagFormType()
}
