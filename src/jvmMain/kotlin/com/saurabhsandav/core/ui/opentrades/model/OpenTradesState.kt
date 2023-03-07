package com.saurabhsandav.core.ui.opentrades.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.closetradeform.CloseTradeFormWindowParams
import com.saurabhsandav.core.ui.opentradeform.OpenTradeFormWindowParams
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorWindowParams

@Immutable
internal data class OpenTradesState(
    val openTrades: List<OpenTradeListEntry>,
    val openTradeFormWindowParams: Collection<OpenTradeFormWindowParams>,
    val pnlCalculatorWindowParams: Collection<PNLCalculatorWindowParams>,
    val closeTradeFormWindowParams: Collection<CloseTradeFormWindowParams>,
)

@Immutable
internal data class OpenTradeListEntry(
    val id: Long,
    val broker: String,
    val ticker: String,
    val quantity: String,
    val side: String,
    val entry: String,
    val stop: String,
    val entryTime: String,
    val target: String,
)
