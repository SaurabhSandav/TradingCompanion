package com.saurabhsandav.core.ui.tags.screen

import com.saurabhsandav.core.trading.ProfileId
import dev.zacsweers.metro.GraphExtension

@GraphExtension
internal interface TagsScreenGraph {

    val presenterFactory: TagsScreenPresenter.Factory

    val profileId: ProfileId

    @GraphExtension.Factory
    interface Factory {

        fun create(): TagsScreenGraph
    }
}
