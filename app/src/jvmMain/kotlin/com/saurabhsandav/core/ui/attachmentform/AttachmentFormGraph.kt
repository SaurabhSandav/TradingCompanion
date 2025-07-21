package com.saurabhsandav.core.ui.attachmentform

import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormType
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension
internal interface AttachmentFormGraph {

    val presenterFactory: AttachmentFormPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(
            @Provides profileId: ProfileId,
            @Provides formType: AttachmentFormType,
        ): AttachmentFormGraph
    }
}
