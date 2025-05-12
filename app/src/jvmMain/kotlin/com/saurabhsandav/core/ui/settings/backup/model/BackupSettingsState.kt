package com.saurabhsandav.core.ui.settings.backup.model

import com.saurabhsandav.core.backup.BackupItem
import com.saurabhsandav.core.backup.service.BackupService
import kotlin.reflect.KClass

internal data class BackupSettingsState(
    val progress: Progress?,
    val services: List<Service>,
    val configuredServices: List<ConfiguredService>,
    val eventSink: (BackupSettingsEvent) -> Unit,
) {

    data class Service(
        val type: ServiceType,
        val name: String,
        val description: String,
    )

    @JvmInline
    value class ServiceType(
        val type: KClass<out BackupService>,
    )

    data class ConfiguredService(
        val id: BackupService.Id,
        val service: String,
        val name: String,
    )

    sealed class Progress {

        data class GeneratingArchive(
            val item: BackupItem,
            val progress: Float,
        ) : Progress()

        data object SavingArchive : Progress()
    }
}
