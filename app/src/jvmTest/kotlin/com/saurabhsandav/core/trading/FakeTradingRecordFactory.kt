package com.saurabhsandav.core.trading

import com.saurabhsandav.core.di.IOCoroutineContext
import com.saurabhsandav.trading.broker.BrokerProvider
import com.saurabhsandav.trading.record.TradingRecord
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, replaces = [AppTradingRecordFactory::class])
@Inject
class FakeTradingRecordFactory(
    @IOCoroutineContext private val coroutineContext: CoroutineContext,
    private val brokerProvider: BrokerProvider,
) : TradingRecordFactory {

    override fun create(
        path: Path,
        onTradeCountsUpdated: suspend (tradeCount: Int, tradeCountOpen: Int) -> Unit,
    ): TradingRecord = TradingRecord(
        coroutineContext = coroutineContext,
        onTradeCountsUpdated = onTradeCountsUpdated,
        brokerProvider = brokerProvider,
    )
}
