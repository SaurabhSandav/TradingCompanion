package com.saurabhsandav.core.ui.attachmentform

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormType
import kotlinx.coroutines.CoroutineScope

internal class AttachmentFormModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter: (
        onCloseRequest: () -> Unit,
        ProfileId,
        AttachmentFormType,
    ) -> AttachmentFormPresenter = { onCloseRequest, profileId, formType ->

        AttachmentFormPresenter(
            onCloseRequest = onCloseRequest,
            coroutineScope = coroutineScope,
            profileId = profileId,
            formType = formType,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
