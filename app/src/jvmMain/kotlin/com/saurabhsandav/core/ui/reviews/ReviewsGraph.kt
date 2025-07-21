package com.saurabhsandav.core.ui.reviews

import dev.zacsweers.metro.GraphExtension

@GraphExtension
internal interface ReviewsGraph {

    val presenterFactory: ReviewsPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(): ReviewsGraph
    }
}
