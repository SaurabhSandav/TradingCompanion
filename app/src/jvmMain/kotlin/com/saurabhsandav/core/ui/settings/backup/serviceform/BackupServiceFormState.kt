package com.saurabhsandav.core.ui.settings.backup.serviceform

import com.saurabhsandav.core.backup.service.BackupService
import com.saurabhsandav.core.ui.common.form.FormModel
import com.saurabhsandav.core.ui.common.form.adapter.addMutableStateField
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import kotlin.reflect.KClass

sealed class BackupServiceFormType {

    data class New(
        val type: KClass<out BackupService>,
    ) : BackupServiceFormType()

    data class Edit(
        val id: BackupService.Id,
    ) : BackupServiceFormType()
}

abstract class BackupServiceFormModel(
    name: String,
) : FormModel() {

    val nameField = addMutableStateField(name) {
        isRequired()
    }
}
