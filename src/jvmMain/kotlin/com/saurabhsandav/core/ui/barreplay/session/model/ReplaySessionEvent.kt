package com.saurabhsandav.core.ui.barreplay.session.model

import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.stockchart.StockChart

internal sealed class ReplaySessionEvent {

    data object ResetReplay : ReplaySessionEvent()

    data object AdvanceReplay : ReplaySessionEvent()

    data object AdvanceReplayByBar : ReplaySessionEvent()

    data class SetIsAutoNextEnabled(val isAutoNextEnabled: Boolean) : ReplaySessionEvent()

    data class ProfileSelected(val id: ProfileId?) : ReplaySessionEvent()

    data class Buy(val stockChart: StockChart) : ReplaySessionEvent()

    data class Sell(val stockChart: StockChart) : ReplaySessionEvent()

    data class CancelOrder(val id: Long) : ReplaySessionEvent()
}
