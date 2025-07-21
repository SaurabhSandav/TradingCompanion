package com.saurabhsandav.core.ui.symbolselectiondialog

import dev.zacsweers.metro.GraphExtension

@GraphExtension
internal interface SymbolSelectionGraph {

    val presenterFactory: SymbolSelectionPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(): SymbolSelectionGraph
    }
}
