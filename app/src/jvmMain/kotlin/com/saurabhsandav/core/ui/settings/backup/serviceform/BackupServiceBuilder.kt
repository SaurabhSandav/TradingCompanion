package com.saurabhsandav.core.ui.settings.backup.serviceform

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.saurabhsandav.core.backup.service.BackupService
import com.saurabhsandav.core.backup.service.BackupService.Id
import kotlin.uuid.Uuid

interface BackupServiceBuilder {

    val formModel: BackupServiceFormModel?

    @Composable
    fun ColumnScope.Form()

    fun init(service: BackupService?)

    fun build(): BackupService
}

internal fun generateBackupServiceId(): Id = Id(Uuid.random().toString())
