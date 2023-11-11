package com.saurabhsandav.core.ui.tradecontent

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.trades.model.TradeTagId

@Immutable
data class ProfileTradeId(
    val profileId: ProfileId,
    val tradeId: TradeId,
)

@Immutable
data class ProfileTradeExecutionId(
    val profileId: ProfileId,
    val executionId: TradeExecutionId,
)

@Immutable
data class ProfileTagId(
    val profileId: ProfileId,
    val tagId: TradeTagId,
)
