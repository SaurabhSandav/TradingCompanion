package com.saurabhsandav.core.ui.pnlcalculator

import kotlinx.coroutines.CoroutineScope

internal class PNLCalculatorModule(
    coroutineScope: CoroutineScope,
) {

    val presenter: () -> PNLCalculatorPresenter = {
        PNLCalculatorPresenter(coroutineScope)
    }
}
