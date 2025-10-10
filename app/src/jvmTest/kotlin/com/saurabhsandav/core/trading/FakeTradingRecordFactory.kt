package com.saurabhsandav.core.trading

import com.saurabhsandav.trading.broker.BrokerProvider
import com.saurabhsandav.trading.record.TradingRecord
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext

class FakeTradingRecordFactory(
    private val coroutineContext: CoroutineContext,
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
