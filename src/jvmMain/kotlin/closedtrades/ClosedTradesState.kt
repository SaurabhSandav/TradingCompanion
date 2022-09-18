package closedtrades

import Side
import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import utils.brokerage

@Stable
internal data class ClosedTradesState(
    val closedTradesDetailed: List<ClosedTradeDetailed>,
)

@Stable
internal data class ClosedTradeDetailed(
    val id: Int,
    val date: String,
    val broker: String,
    val ticker: String,
    val instrument: String,
    val quantity: String,
    val side: String,
    val entry: String,
    val stop: String?,
    val entryTime: String,
    val target: String?,
    val exit: String,
    val exitTime: String,
    val maxFavorableExcursion: String,
    val maxAdverseExcursion: String,
    val persisted: Int?,
    val persistenceResult: String?,
) {

    val pnl: String

    val netPnl: String

    val fees: String

    val rValue: String

    val duration: String

    init {

        val entryBD = entry.toBigDecimal()
        val stopBD = stop?.toBigDecimal()
        val exitBD = exit.toBigDecimal()
        val quantityBD = quantity.toBigDecimal()
        val side = Side.fromString(side)

        val pnlBD = when (side) {
            Side.Long -> (exitBD - entryBD) * quantityBD
            Side.Short -> (entryBD - exitBD) * quantityBD
        }

        val netPnlBD = brokerage(
            broker = broker,
            instrument = instrument,
            entry = entryBD,
            exit = exitBD,
            quantity = quantityBD,
            side = side,
        )

        pnl = pnlBD.toPlainString()
        netPnl = netPnlBD.toPlainString()
        fees = (pnlBD - netPnlBD).toPlainString()

        rValue = when (stopBD) {
            null -> "NA"
            else -> when (side) {
                Side.Long -> pnlBD / ((entryBD - stopBD) * quantityBD)
                Side.Short -> (pnlBD / ((stopBD - entryBD) * quantityBD))
            }.toPlainString()
        }

        val timeZone = TimeZone.of("Asia/Kolkata")
        val entryInstant = LocalDateTime.parse(entryTime).toInstant(timeZone)
        val exitInstant = LocalDateTime.parse(exitTime).toInstant(timeZone)
        val s = (exitInstant - entryInstant).inWholeSeconds

        duration = "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, (s % 60))
    }
}
