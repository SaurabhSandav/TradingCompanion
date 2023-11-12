package com.saurabhsandav.core.ui.profiles.form

import com.saurabhsandav.core.AppModule
import kotlinx.coroutines.CoroutineScope

internal class ProfileFormModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {
            formType: ProfileFormType,
            onCloseRequest: () -> Unit,
        ->

        ProfileFormPresenter(
            coroutineScope = coroutineScope,
            formType = formType,
            onCloseRequest = onCloseRequest,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
