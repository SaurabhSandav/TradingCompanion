package com.saurabhsandav.core.ui.profiles.form

import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension
internal interface ProfileFormGraph {

    val presenterFactory: ProfileFormPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(
            @Provides formType: ProfileFormType,
        ): ProfileFormGraph
    }
}
