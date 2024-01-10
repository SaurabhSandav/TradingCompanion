package com.saurabhsandav.core.ui.tagform

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.tagform.model.TagFormType
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
