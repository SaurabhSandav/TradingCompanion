package com.saurabhsandav.core.ui.tradeexecutionform.model

import com.saurabhsandav.core.ui.common.form.FormValidator

internal sealed class OrderFormType {

    data class New(val formModel: ((FormValidator) -> OrderFormModel)? = null) : OrderFormType()

    data class NewFromExisting(val id: Long) : OrderFormType()

    data class Edit(val id: Long) : OrderFormType()
}
