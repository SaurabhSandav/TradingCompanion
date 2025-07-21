package com.saurabhsandav.core.ui.pnlcalculator

import dev.zacsweers.metro.GraphExtension

@GraphExtension
internal interface PNLCalculatorGraph {

    val presenterFactory: PNLCalculatorPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(): PNLCalculatorGraph
    }
}
