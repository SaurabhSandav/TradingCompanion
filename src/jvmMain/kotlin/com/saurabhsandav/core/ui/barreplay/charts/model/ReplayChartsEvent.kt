package com.saurabhsandav.core.ui.barreplay.charts.model

sealed class ReplayChartsEvent {

    object Reset : ReplayChartsEvent()

    object Next : ReplayChartsEvent()

    data class ChangeIsAutoNextEnabled(val isAutoNextEnabled: Boolean) : ReplayChartsEvent()
}
