package com.saurabhsandav.core.ui.sizing

import dev.zacsweers.metro.GraphExtension

@GraphExtension
internal interface SizingGraph {

    val presenterFactory: SizingPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(): SizingGraph
    }
}
