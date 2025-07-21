package com.saurabhsandav.core.ui.settings.backup

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.backup.BackupEvent
import com.saurabhsandav.core.backup.BackupManager
import com.saurabhsandav.core.backup.BackupServicesManager
import com.saurabhsandav.core.backup.RestoreScheduler
import com.saurabhsandav.core.backup.service.BackupService
import com.saurabhsandav.core.backup.service.LocalBackupService
import com.saurabhsandav.core.backup.service.RcloneBackupService
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent.Backup
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent.DeleteService
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent.Restore
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.Progress
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.ServiceType
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.copyTo

@AssistedInject
internal class BackupSettingsPresenter(
    @Assisted private val coroutineScope: CoroutineScope,
    private val backupManager: BackupManager,
    private val restoreScheduler: RestoreScheduler,
    private val backupServicesManager: BackupServicesManager,
) {

    private var progress by mutableStateOf<Progress?>(null)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule BackupSettingsState(
            progress = progress,
            services = remember { BackupServicesDetails.values.toList() },
            configuredServices = remember {
                backupServicesManager.services.mapList { service ->

                    BackupSettingsState.ConfiguredService(
                        id = service.id,
                        service = getBackupServiceDetails(service).name,
                        name = service.name,
                    )
                }
            }.collectAsState(emptyList()).value,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: BackupSettingsEvent) {

        when (event) {
            is Backup -> onBackup(event.toDirPath)
            is Restore -> onRestore(event.archivePath)
            is DeleteService -> onDeleteService(event.id)
            is BackupSettingsEvent.BackupToService -> onBackupToService(event.id)
        }
    }

    private fun getBackupServiceDetails(service: BackupService): BackupSettingsState.Service {

        return BackupServicesDetails.getOrElse(service::class) {
            error("BackupService of type ${service::class.qualifiedName} not found")
        }
    }

    private fun onBackup(toDirPath: String) = coroutineScope.launchUnit {

        performBackup { archivePath ->
            archivePath.copyTo(Path(toDirPath).resolve(archivePath.fileName))
        }
    }

    private fun onRestore(archivePath: String) {
        restoreScheduler.schedule(Path(archivePath))
    }

    private fun onDeleteService(id: BackupService.Id) = coroutineScope.launchUnit {
        backupServicesManager.deleteService(id)
    }

    private fun onBackupToService(id: BackupService.Id) = coroutineScope.launchUnit {

        performBackup { archivePath ->
            val instance = backupServicesManager.getService(id).newInstance()
            instance.saveBackup(archivePath)
        }
    }

    private suspend fun performBackup(onSaveArchive: suspend (Path) -> Unit) {

        backupManager.backup(
            onProgress = { event ->

                progress = when (event) {
                    is BackupEvent.GeneratingArchive -> Progress.GeneratingArchive(
                        item = event.item ?: return@backup,
                        progress = event.copied / event.size.toFloat(),
                    )

                    BackupEvent.SavingArchive -> Progress.SavingArchive
                    BackupEvent.Finished -> null
                }
            },
            onSaveArchive = onSaveArchive,
        )
    }

    @AssistedFactory
    fun interface Factory {

        fun create(coroutineScope: CoroutineScope): BackupSettingsPresenter
    }
}

private val BackupServicesDetails = mapOf(
    LocalBackupService::class to BackupSettingsState.Service(
        type = ServiceType(LocalBackupService::class),
        name = "Local",
        description = "Save to local filesystem",
    ),
    RcloneBackupService::class to BackupSettingsState.Service(
        type = ServiceType(RcloneBackupService::class),
        name = "Rclone",
        description = "Use Rclone to save backup",
    ),
)
