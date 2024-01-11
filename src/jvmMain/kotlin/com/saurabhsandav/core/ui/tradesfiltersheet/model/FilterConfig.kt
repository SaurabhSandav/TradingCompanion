package com.saurabhsandav.core.ui.tradesfiltersheet.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.autoSaver
import com.saurabhsandav.core.trades.model.*

@Immutable
data class FilterConfig(
    val openClosed: OpenClosed = OpenClosed.All,
    val side: Side = Side.All,
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
    }

    enum class OpenClosed { All, Open, Closed }

    enum class Side { All, Long, Short }

    companion object {

        val Saver = autoSaver<FilterConfig>()
    }
}
