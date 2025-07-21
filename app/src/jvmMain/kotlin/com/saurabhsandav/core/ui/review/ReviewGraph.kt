package com.saurabhsandav.core.ui.review

import com.saurabhsandav.core.ui.tradecontent.ProfileReviewId
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension
internal interface ReviewGraph {

    val presenterFactory: ReviewPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(
            @Provides profileReviewId: ProfileReviewId,
        ): ReviewGraph
    }
}
