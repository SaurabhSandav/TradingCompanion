package com.saurabhsandav.core.ui.tags.form.model

import com.saurabhsandav.core.trading.record.model.TradeTagId

sealed class TagFormType {

    data class New(
        val name: String? = null,
    ) : TagFormType()

    data class NewFromExisting(
        val id: TradeTagId,
    ) : TagFormType()

    data class Edit(
        val id: TradeTagId,
    ) : TagFormType()
}
