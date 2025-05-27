package com.saurabhsandav.core.backup

import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.backup.service.BackupService
import com.saurabhsandav.core.backup.service.LocalBackupService
import com.saurabhsandav.core.backup.service.RcloneBackupService
import com.saurabhsandav.core.utils.PrefKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

class BackupServicesManager(
    private val appPrefs: FlowSettings,
    private val servicesSerializersModule: SerializersModule = EmptySerializersModule(),
) {

    private val module = SerializersModule {
        polymorphic(BackupService::class) {
            subclass(LocalBackupService::class)
            subclass(RcloneBackupService::class)
        }
    }
    private val format = Json { serializersModule = module + servicesSerializersModule }

    val services: Flow<List<BackupService>> = appPrefs
        .getStringOrNullFlow(PrefKeys.BackupServices)
        .map { serialized -> serialized?.let(format::decodeFromString) ?: emptyList() }

    suspend fun addService(service: BackupService) {

        editServices { services ->

            when (val index = services.indexOfFirst { it.id == service.id }) {
                -1 -> services + service
                else -> services.toMutableList().also { it[index] = service }
            }
        }
    }

    suspend fun deleteService(id: BackupService.Id) {

        editServices { services ->

            when (val index = services.indexOfFirst { it.id == id }) {
                -1 -> services
                else -> services.toMutableList().also { it.removeAt(index) }
            }
        }
    }

    private suspend fun editServices(block: (List<BackupService>) -> List<BackupService>) {

        val services = services.first()
        val newServices = block(services)

        val serialized = format.encodeToString(newServices)

        appPrefs.putString(PrefKeys.BackupServices, serialized)
    }

    suspend fun getService(id: BackupService.Id): BackupService {

        val service = services.first().find { it.id == id }

        requireNotNull(service) { "BackupService with id ($id) does not exist" }

        return service
    }
}
