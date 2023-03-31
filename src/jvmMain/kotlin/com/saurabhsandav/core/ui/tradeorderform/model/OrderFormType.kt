package com.saurabhsandav.core.ui.tradeorderform.model

sealed class OrderFormType {

    object New : OrderFormType()

    data class NewFromExisting(val id: Long) : OrderFormType()

    data class Edit(val id: Long) : OrderFormType()
}
