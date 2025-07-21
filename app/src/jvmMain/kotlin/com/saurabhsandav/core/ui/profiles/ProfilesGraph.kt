package com.saurabhsandav.core.ui.profiles

import dev.zacsweers.metro.GraphExtension

@GraphExtension
internal interface ProfilesGraph {

    val presenterFactory: ProfilesPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(): ProfilesGraph
    }
}
