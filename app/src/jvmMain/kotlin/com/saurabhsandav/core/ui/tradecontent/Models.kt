package com.saurabhsandav.core.ui.tradecontent

import com.saurabhsandav.core.trading.record.model.ProfileId
import com.saurabhsandav.core.trading.record.model.ReviewId
import com.saurabhsandav.core.trading.record.model.TradeExecutionId
import com.saurabhsandav.core.trading.record.model.TradeId
import com.saurabhsandav.core.trading.record.model.TradeTagId

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
