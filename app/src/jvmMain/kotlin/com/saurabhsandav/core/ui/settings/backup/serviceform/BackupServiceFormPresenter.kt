package com.saurabhsandav.core.ui.settings.backup.serviceform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.backup.BackupServicesManager
import com.saurabhsandav.core.backup.service.BackupService
import com.saurabhsandav.core.backup.service.LocalBackupService
import com.saurabhsandav.core.backup.service.RcloneBackupService
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormType.Edit
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormType.New
import com.saurabhsandav.core.ui.settings.backup.serviceform.service.LocalBackupServiceBuilder
import com.saurabhsandav.core.ui.settings.backup.serviceform.service.RcloneBackupServiceBuilder
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

internal class BackupServiceFormPresenter(
    private val onDismissRequest: () -> Unit,
    private val coroutineScope: CoroutineScope,
    private val formType: BackupServiceFormType,
    private val backupServicesManager: BackupServicesManager,
) {

    var serviceBuilder by mutableStateOf<BackupServiceBuilder?>(null)

    init {

        when (formType) {
            is New -> new(formType.type)
            is Edit -> edit(formType.id)
        }
    }

    fun onSubmit() = coroutineScope.launchUnit {

        val service = serviceBuilder!!.build()

        backupServicesManager.addService(service)

        // Close form
        onDismissRequest()
    }

    private fun new(service: KClass<out BackupService>) = coroutineScope.launchUnit {

        serviceBuilder = getConfigBuilder(service, formType).apply { init(null) }
    }

    private fun edit(id: BackupService.Id) = coroutineScope.launchUnit {

        val service = backupServicesManager.getService(id)
        serviceBuilder = getConfigBuilder(service::class, formType).apply { init(service) }
    }

    private fun getConfigBuilder(
        serviceType: KClass<out BackupService>,
        type: BackupServiceFormType,
    ): BackupServiceBuilder = when (serviceType) {
        LocalBackupService::class -> LocalBackupServiceBuilder(type)
        RcloneBackupService::class -> RcloneBackupServiceBuilder(type)
        else -> error("BackupService of type ${serviceType.qualifiedName} not found")
    }
}
