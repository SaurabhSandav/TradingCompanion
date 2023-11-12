package com.saurabhsandav.core.ui.tags.form

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.model.ProfileId
import kotlinx.coroutines.CoroutineScope

internal class TagFormModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {
            profileId: ProfileId,
            formType: TagFormType,
            onCloseRequest: () -> Unit,
        ->

        TagFormPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            formType = formType,
            onCloseRequest = onCloseRequest,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
