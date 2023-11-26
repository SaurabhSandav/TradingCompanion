package com.saurabhsandav.core.ui.profiles.form

import com.saurabhsandav.core.AppModule
import kotlinx.coroutines.CoroutineScope

internal class ProfileFormModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {
            onCloseRequest: () -> Unit,
            formType: ProfileFormType,
            trainingOnly: Boolean,
        ->

        ProfileFormPresenter(
            coroutineScope = coroutineScope,
            onCloseRequest = onCloseRequest,
            formType = formType,
            trainingOnly = trainingOnly,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
