package ui.sizing

import AppModule
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.SizingTrade
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mapList
import model.Account
import model.Side
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

internal class SizingPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
) {

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        val account by appModule.account.collectAsState(
            Account(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
            )
        )

        val sizedTrades by remember {
            appModule.appDB.sizingTradeQueries
                .getAllNotHidden()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .mapList { sizingTrade -> sizingTrade.size(account) }
        }.collectAsState(emptyList())

        return@launchMolecule SizingState(
            sizedTrades = sizedTrades
        )
    }

    internal fun updateEntry(sizedTrade: SizedTrade, entry: String) =
        updateSizingTrade(sizedTrade, entry, sizedTrade.stop)

    internal fun updateStop(sizedTrade: SizedTrade, stop: String) =
        updateSizingTrade(sizedTrade, sizedTrade.entry, stop)

    internal fun addTrade(ticker: String) {

        coroutineScope.launch(Dispatchers.IO) {
            appModule.appDB.sizingTradeQueries.insert(
                SizingTrade(
                    ticker = ticker,
                    entry = "0",
                    stop = "0",
                    hidden = false.toString(),
                )
            )
        }
    }

    internal fun removeTrade(ticker: String) {

        coroutineScope.launch(Dispatchers.IO) {
            appModule.appDB.sizingTradeQueries.hide(ticker)
        }
    }

    private fun updateSizingTrade(sizedTrade: SizedTrade, entry: String, stop: String) {

        if (entry.toBigDecimalOrNull() == null || stop.toBigDecimalOrNull() == null) return

        coroutineScope.launch(Dispatchers.IO) {
            appModule.appDB.sizingTradeQueries.update(
                ticker = sizedTrade.ticker,
                entry = entry,
                stop = stop,
            )
        }
    }

    private fun SizingTrade.size(account: Account): SizedTrade {

        val entryBD = entry.toBigDecimal()
        val stopBD = stop.toBigDecimal()

        val entryStopComparison = entryBD.compareTo(stopBD)

        val spread = (entryBD - stopBD).abs()

        val calculatedQuantity = when {
            spread.compareTo(BigDecimal.ZERO) == 0 -> BigDecimal.ZERO
            else -> (account.riskAmount / spread).setScale(0, RoundingMode.FLOOR)
        }

        val maxAffordableQuantity = when {
            entryBD.compareTo(BigDecimal.ZERO) == 0 -> BigDecimal.ZERO
            else -> (account.balancePerTrade * account.leverage) / entryBD
        }

        return SizedTrade(
            ticker = ticker,
            entry = entry,
            stop = stop,
            side = when {
                entryStopComparison > 0 -> Side.Long.strValue
                entryStopComparison < 0 -> Side.Short.strValue
                else -> "Invalid"
            }.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            spread = spread.toPlainString(),
            calculatedQuantity = calculatedQuantity.toPlainString(),
            maxAffordableQuantity = maxAffordableQuantity.toPlainString(),
            entryQuantity = calculatedQuantity.min(maxAffordableQuantity).toPlainString(),
            target = when {
                entry > stop -> entryBD + spread // Long
                else -> entryBD - spread // Short
            }.toPlainString(),
        )
    }
}
