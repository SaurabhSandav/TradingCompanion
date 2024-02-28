package com.saurabhsandav.core.ui.tradecontent

import com.saurabhsandav.core.trades.model.*

data class ProfileTradeId(
    val profileId: ProfileId,
    val tradeId: TradeId,
)

data class ProfileTradeExecutionId(
    val profileId: ProfileId,
    val executionId: TradeExecutionId,
)

data class ProfileTagId(
    val profileId: ProfileId,
    val tagId: TradeTagId,
)

data class ProfileReviewId(
    val profileId: ProfileId,
    val reviewId: ReviewId,
)
