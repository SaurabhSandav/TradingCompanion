package com.saurabhsandav.core.ui.tags.model

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.trades.model.TradeTagId

data class TradeTag(
    val id: TradeTagId,
    val name: String,
    val description: String?,
    val color: Color?,
)
