package com.saurabhsandav.core.trades

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.utils.BigDecimalColumnAdapter
import com.saurabhsandav.core.utils.InstantColumnAdapter
import com.saurabhsandav.core.utils.LocalDateTimeColumnAdapter
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
                lotsAdapter = IntColumnAdapter,
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
            TradeExecutionAdapter = TradeExecution.Adapter(
                instrumentAdapter = Instrument.ColumnAdapter,
                quantityAdapter = BigDecimalColumnAdapter,
                lotsAdapter = IntColumnAdapter,
                sideAdapter = TradeExecutionSide.ColumnAdapter,
                priceAdapter = BigDecimalColumnAdapter,
                timestampAdapter = LocalDateTimeColumnAdapter,
            ),
            TradeToExecutionMapAdapter = TradeToExecutionMap.Adapter(
                overrideQuantityAdapter = BigDecimalColumnAdapter,
            ),
            TradeStopAdapter = TradeStop.Adapter(
                priceAdapter = BigDecimalColumnAdapter,
            ),
            TradeTargetAdapter = TradeTarget.Adapter(
                priceAdapter = BigDecimalColumnAdapter,
            ),
            TradeNoteAdapter = TradeNote.Adapter(
                addedAdapter = InstantColumnAdapter,
                lastEditedAdapter = InstantColumnAdapter,
            ),
        )
    }

    val executions = TradeExecutionsRepo(tradesDB)

    val trades = TradesRepo(recordPath, tradesDB, executions, candleRepo)

    val sizingTrades = SizingTradesRepo(tradesDB)
}
