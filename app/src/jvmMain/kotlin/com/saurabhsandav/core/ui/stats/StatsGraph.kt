package com.saurabhsandav.core.ui.stats

import dev.zacsweers.metro.GraphExtension

@GraphExtension(StatsGraph::class)
internal interface StatsGraph {

    val presenterFactory: StatsPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(): StatsGraph
    }
}
