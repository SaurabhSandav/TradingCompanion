package sizing

import Account
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import mapList
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.coroutines.CoroutineContext

internal class SizingPresenter(
    private val appModule: AppModule,
) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob() as CoroutineContext)

    val state = coroutineScope.launchMolecule(RecompositionClock.Immediate) {

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
                .getAll()
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
                    added = Clock.System.now().toString(),
                )
            )
        }
    }

    internal fun removeTrade(ticker: String) {

        coroutineScope.launch(Dispatchers.IO) {
            appModule.appDB.sizingTradeQueries.delete(ticker)
        }
    }

    private fun updateSizingTrade(sizedTrade: SizedTrade, entry: String, stop: String) {

        if (entry.toBigDecimalOrNull() == null || stop.toBigDecimalOrNull() == null) return

        coroutineScope.launch(Dispatchers.IO) {
            appModule.appDB.sizingTradeQueries.insert(
                SizingTrade(
                    ticker = sizedTrade.ticker,
                    entry = entry,
                    stop = stop,
                    added = sizedTrade.added,
                )
            )
        }
    }

    private fun SizingTrade.size(account: Account): SizedTrade {

        val entryBD = entry.toBigDecimal()
        val spread = (entryBD - stop.toBigDecimal()).abs()
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
            added = added,
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
