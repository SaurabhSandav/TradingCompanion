package com.saurabhsandav.core.ui.tradesfiltersheet.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.autoSaver
import com.saurabhsandav.core.trades.model.*
import java.math.BigDecimal

@Immutable
data class FilterConfig(
    val openClosed: OpenClosed = OpenClosed.All,
    val side: Side = Side.All,
    val pnl: PNL = PNL.All,
    val filterByNetPnl: Boolean = false,
) {

    fun toTradeFilter(): TradeFilter = TradeFilter {

        when (openClosed) {
            OpenClosed.All -> Unit
            OpenClosed.Open -> isOpen()
            OpenClosed.Closed -> isClosed()
        }

        when (side) {
            Side.All -> Unit
            Side.Long -> isLong()
            Side.Short -> isShort()
        }

        when (pnl) {
            PNL.All -> Unit
            PNL.Breakeven -> pnlRange(from = BigDecimal.ZERO, to = BigDecimal.ZERO, filterByNetPnl = filterByNetPnl)
            PNL.Profit -> pnlRange(from = BigDecimal.ZERO, filterByNetPnl = filterByNetPnl)
            PNL.Loss -> pnlRange(to = BigDecimal.ZERO, filterByNetPnl = filterByNetPnl)
            is PNL.Custom -> with(pnl) {
                if (from == null || to == null || from <= to)
                    pnlRange(from = from, to = to, filterByNetPnl = filterByNetPnl)
            }
        }
    }

    enum class OpenClosed { All, Open, Closed }

    enum class Side { All, Long, Short }

    sealed class PNL {

        data object All : PNL()
        data object Breakeven : PNL()
        data object Profit : PNL()
        data object Loss : PNL()

        data class Custom(
            val from: BigDecimal? = null,
            val to: BigDecimal? = null,
        ) : PNL()
    }

    companion object {

        val Saver = autoSaver<FilterConfig>()
    }
}
