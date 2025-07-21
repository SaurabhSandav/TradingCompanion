package com.saurabhsandav.core.ui.barreplay

import com.saurabhsandav.core.ui.barreplay.session.ReplaySessionGraph
import dev.zacsweers.metro.GraphExtension

@GraphExtension
internal interface BarReplayGraph {

    val presenterFactory: BarReplayPresenter.Factory

    val replaySessionGraphFactory: ReplaySessionGraph.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(): BarReplayGraph
    }
}
