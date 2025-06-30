package com.saurabhsandav.core.ui.tradesfiltersheet.model

import androidx.compose.runtime.saveable.autoSaver
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.model.TradeFilter
import com.saurabhsandav.trading.record.model.TradeTagId
import com.saurabhsandav.trading.record.model.hasNotes
import com.saurabhsandav.trading.record.model.instantRange
import com.saurabhsandav.trading.record.model.isClosed
import com.saurabhsandav.trading.record.model.isLong
import com.saurabhsandav.trading.record.model.isOpen
import com.saurabhsandav.trading.record.model.isShort
import com.saurabhsandav.trading.record.model.noNotes
import com.saurabhsandav.trading.record.model.pnlRange
import com.saurabhsandav.trading.record.model.symbols
import com.saurabhsandav.trading.record.model.tags
import com.saurabhsandav.trading.record.model.timeRange
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import kotlin.time.Clock

data class FilterConfig(
    val openClosed: OpenClosed = OpenClosed.All,
    val side: Side = Side.All,
    val dateInterval: DateInterval = DateInterval.All,
    val timeInterval: TimeInterval = TimeInterval.All,
    val pnl: PNL = PNL.All,
    val filterByNetPnl: Boolean = false,
    val notes: Notes = Notes.All,
    val tags: List<TradeTagId> = emptyList(),
    val matchAllTags: Boolean = false,
    val symbols: List<SymbolId> = emptyList(),
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

        when (dateInterval) {
            DateInterval.All -> Unit
            is DateInterval.Custom -> with(dateInterval) {

                if (from == null || to == null || from <= to) {

                    val tz = TimeZone.currentSystemDefault()

                    instantRange(
                        from = from?.atStartOfDayIn(tz),
                        to = to?.plus(DatePeriod(days = 1))?.atStartOfDayIn(tz),
                    )
                }
            }

            else -> {

                val tz = TimeZone.currentSystemDefault()
                val today = Clock.System.now().toLocalDateTime(tz).date

                val todayOffset = when (dateInterval) {
                    DateInterval.Today -> DatePeriod()
                    DateInterval.ThisWeek -> DatePeriod(days = today.dayOfWeek.isoDayNumber - 1)
                    DateInterval.ThisMonth -> DatePeriod(days = today.day - 1)
                    DateInterval.ThisYear -> DatePeriod(months = today.month.number - 1, days = today.day - 1)
                    else -> error("DateInterval should've already been handled.")
                }

                instantRange(from = today.minus(todayOffset).atStartOfDayIn(tz))
            }
        }

        when (timeInterval) {
            TimeInterval.All -> Unit
            is TimeInterval.Custom -> with(timeInterval) {

                if (from == null || to == null || from <= to) {
                    timeRange(from = from, to = to)
                }
            }
        }

        when (pnl) {
            PNL.All -> Unit
            PNL.Breakeven -> pnlRange(from = BigDecimal.ZERO, to = BigDecimal.ZERO, filterByNetPnl = filterByNetPnl)
            PNL.Profit -> pnlRange(from = BigDecimal.ZERO, filterByNetPnl = filterByNetPnl)
            PNL.Loss -> pnlRange(to = BigDecimal.ZERO, filterByNetPnl = filterByNetPnl)
            is PNL.Custom -> with(pnl) {
                if (from == null || to == null || from <= to) {
                    pnlRange(from = from, to = to, filterByNetPnl = filterByNetPnl)
                }
            }
        }

        when (notes) {
            Notes.All -> Unit
            Notes.HasNotes -> hasNotes()
            Notes.NoNotes -> noNotes()
        }

        tags(tags, matchAllTags)

        symbols(symbols)
    }

    enum class OpenClosed { All, Open, Closed }

    enum class Side { All, Long, Short }

    sealed class DateInterval {

        data object All : DateInterval()

        data object Today : DateInterval()

        data object ThisWeek : DateInterval()

        data object ThisMonth : DateInterval()

        data object ThisYear : DateInterval()

        data class Custom(
            val from: LocalDate? = null,
            val to: LocalDate? = null,
        ) : DateInterval()
    }

    sealed class TimeInterval {

        data object All : TimeInterval()

        data class Custom(
            val from: LocalTime? = null,
            val to: LocalTime? = null,
        ) : TimeInterval()
    }

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

    enum class Notes { All, HasNotes, NoNotes }

    companion object {

        val Saver = autoSaver<FilterConfig>()
    }
}
