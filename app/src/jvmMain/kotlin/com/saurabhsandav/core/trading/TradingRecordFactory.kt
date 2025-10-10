package com.saurabhsandav.core.trading

import com.saurabhsandav.core.di.IOCoroutineContext
import com.saurabhsandav.trading.broker.BrokerProvider
import com.saurabhsandav.trading.record.TradingRecord
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.absolutePathString

fun interface TradingRecordFactory {

    fun create(
        path: Path,
        onTradeCountsUpdated: suspend (tradeCount: Int, tradeCountOpen: Int) -> Unit,
    ): TradingRecord
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class AppTradingRecordFactory(
    @IOCoroutineContext private val coroutineContext: CoroutineContext,
    private val brokerProvider: BrokerProvider,
    private val symbolsProvider: SymbolsProvider? = null,
) : TradingRecordFactory {

    override fun create(
        path: Path,
        onTradeCountsUpdated: suspend (tradeCount: Int, tradeCountOpen: Int) -> Unit,
    ): TradingRecord = TradingRecord(
        coroutineContext = coroutineContext,
        dbUrl = "jdbc:sqlite:${path.absolutePathString()}/Trades.db",
        attachmentsDir = path.resolve("attachments"),
        brokerProvider = brokerProvider,
        getSymbol = symbolsProvider?.let {
            { brokerId, symbolId ->
                symbolsProvider.getSymbol(brokerId, symbolId).first()?.toRecordSymbol()
            }
        },
        onTradeCountsUpdated = onTradeCountsUpdated,
    )
}
