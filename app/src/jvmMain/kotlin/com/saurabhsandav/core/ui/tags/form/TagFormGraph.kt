package com.saurabhsandav.core.ui.tags.form

import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.tags.form.model.TagFormType
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension
internal interface TagFormGraph {

    val presenterFactory: TagFormPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(
            @Provides profileId: ProfileId,
            @Provides formType: TagFormType,
        ): TagFormGraph
    }
}
