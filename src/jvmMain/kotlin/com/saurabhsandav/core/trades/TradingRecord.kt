package com.saurabhsandav.core.trades

import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.OrderType
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.utils.BigDecimalColumnAdapter
import com.saurabhsandav.core.utils.InstantColumnAdapter
import com.saurabhsandav.core.utils.LocalDateTimeColumnAdapter
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import java.util.*

internal class TradingRecord(
    recordPath: String,
    candleRepo: CandleRepository,
) {

    private val tradesDB: TradesDB = run {
        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:$recordPath/Trades.db",
            properties = Properties().apply { put("foreign_keys", "true") },
        )
        TradesDB.Schema.create(driver)
        TradesDB(
            driver = driver,
            SizingTradeAdapter = SizingTrade.Adapter(
                entryAdapter = BigDecimalColumnAdapter,
                stopAdapter = BigDecimalColumnAdapter,
            ),
            TradeAdapter = Trade.Adapter(
                instrumentAdapter = Instrument.ColumnAdapter,
                quantityAdapter = BigDecimalColumnAdapter,
                closedQuantityAdapter = BigDecimalColumnAdapter,
                sideAdapter = TradeSide.ColumnAdapter,
                averageEntryAdapter = BigDecimalColumnAdapter,
                entryTimestampAdapter = LocalDateTimeColumnAdapter,
                averageExitAdapter = BigDecimalColumnAdapter,
                exitTimestampAdapter = LocalDateTimeColumnAdapter,
                pnlAdapter = BigDecimalColumnAdapter,
                feesAdapter = BigDecimalColumnAdapter,
                netPnlAdapter = BigDecimalColumnAdapter,
            ),
            TradeMfeMaeAdapter = TradeMfeMae.Adapter(
                mfePriceAdapter = BigDecimalColumnAdapter,
                maePriceAdapter = BigDecimalColumnAdapter,
            ),
            TradeOrderAdapter = TradeOrder.Adapter(
                instrumentAdapter = Instrument.ColumnAdapter,
                quantityAdapter = BigDecimalColumnAdapter,
                typeAdapter = OrderType.OrderTypeColumnAdapter,
                priceAdapter = BigDecimalColumnAdapter,
                timestampAdapter = LocalDateTimeColumnAdapter,
            ),
            TradeToOrderMapAdapter = TradeToOrderMap.Adapter(
                overrideQuantityAdapter = BigDecimalColumnAdapter,
            ),
            TradeStopAdapter = TradeStop.Adapter(
                priceAdapter = BigDecimalColumnAdapter,
                riskAdapter = BigDecimalColumnAdapter,
            ),
            TradeTargetAdapter = TradeTarget.Adapter(
                priceAdapter = BigDecimalColumnAdapter,
                profitAdapter = BigDecimalColumnAdapter,
            ),
            TradeNoteAdapter = TradeNote.Adapter(
                addedAdapter = InstantColumnAdapter,
                lastEditedAdapter = InstantColumnAdapter,
            ),
        )
    }

    val orders = TradeOrdersRepo(tradesDB)

    val trades = TradesRepo(tradesDB, orders, candleRepo)

    val sizingTrades = SizingTradesRepo(tradesDB)
}
