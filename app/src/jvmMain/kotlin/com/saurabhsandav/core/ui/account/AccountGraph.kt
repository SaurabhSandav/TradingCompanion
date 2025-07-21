package com.saurabhsandav.core.ui.account

import dev.zacsweers.metro.GraphExtension

@GraphExtension
internal interface AccountGraph {

    val presenterFactory: AccountPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(): AccountGraph
    }
}
