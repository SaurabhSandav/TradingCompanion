package com.saurabhsandav.core.ui.pnlcalculator.model

internal sealed class PNLCalculatorEvent {

    data object Calculate : PNLCalculatorEvent()

    data class RemoveCalculation(
        val id: Int,
    ) : PNLCalculatorEvent()
}
