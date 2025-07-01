package com.saurabhsandav.core.ui.tags.form

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.tags.form.model.TagFormType
import kotlinx.coroutines.CoroutineScope

internal class TagFormModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter: (
        onCloseRequest: () -> Unit,
        ProfileId,
        TagFormType,
    ) -> TagFormPresenter = { onCloseRequest, profileId, formType ->

        TagFormPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            formType = formType,
            onCloseRequest = onCloseRequest,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
