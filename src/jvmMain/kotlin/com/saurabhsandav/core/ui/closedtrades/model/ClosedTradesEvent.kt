package com.saurabhsandav.core.ui.closedtrades.model

internal sealed class ClosedTradesEvent {

    data class DeleteTrade(val id: Long) : ClosedTradesEvent()

    sealed class DeleteConfirmationDialog : ClosedTradesEvent() {

        data class Confirm(val id: Long) : DeleteConfirmationDialog()

        object Dismiss : DeleteConfirmationDialog()
    }

    data class OpenChart(val id: Long) : ClosedTradesEvent()

    data class EditTrade(val id: Long) : ClosedTradesEvent()

    data class OpenPNLCalculator(val id: Long) : ClosedTradesEvent()
}
