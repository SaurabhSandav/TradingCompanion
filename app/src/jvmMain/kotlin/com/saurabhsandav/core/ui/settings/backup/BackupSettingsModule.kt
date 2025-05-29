package com.saurabhsandav.core.ui.settings.backup

import com.saurabhsandav.core.backup.BackupServicesManager
import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormPresenter
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormType
import kotlinx.coroutines.CoroutineScope

internal class BackupSettingsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter: () -> BackupSettingsPresenter = {

        BackupSettingsPresenter(
            coroutineScope = coroutineScope,
            backupManager = appModule.backupManager,
            restoreScheduler = appModule.restoreScheduler,
            backupServicesManager = BackupServicesManager(
                appPrefs = appModule.appPrefs,
            ),
        )
    }

    val serviceFormPresenter: (
        onDismissRequest: () -> Unit,
        BackupServiceFormType,
    ) -> BackupServiceFormPresenter = { onDismissRequest, formType ->

        BackupServiceFormPresenter(
            onDismissRequest = onDismissRequest,
            coroutineScope = coroutineScope,
            formType = formType,
            backupServicesManager = BackupServicesManager(
                appPrefs = appModule.appPrefs,
            ),
        )
    }
}
