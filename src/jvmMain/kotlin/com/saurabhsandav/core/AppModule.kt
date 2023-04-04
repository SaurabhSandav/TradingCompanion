package com.saurabhsandav.core

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import com.saurabhsandav.core.fyers_api.FyersApi
import com.saurabhsandav.core.trades.TradeOrdersRepo
import com.saurabhsandav.core.trades.TradesRepo
import com.saurabhsandav.core.trades.model.Account
import com.saurabhsandav.core.trades.model.OrderType
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.trading.data.CandleCacheDB
import com.saurabhsandav.core.trading.data.CandleDBCollection
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.trading.data.FyersCandleDownloader
import com.saurabhsandav.core.utils.AppPaths
import com.saurabhsandav.core.utils.BigDecimalColumnAdapter
import com.saurabhsandav.core.utils.InstantColumnAdapter
import com.saurabhsandav.core.utils.LocalDateTimeColumnAdapter
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.*
import java.util.prefs.Preferences

internal class AppModule {

    val account: Flow<Account> = flowOf(
        Account(
            balance = 11250.toBigDecimal(),
            balancePerTrade = 11250.toBigDecimal(),
            leverage = 5.toBigDecimal(),
            riskAmount = 11250.toBigDecimal() * 0.02.toBigDecimal(),
        )
    )

    val appDB: AppDB = run {
        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:${AppPaths.getAppDataPath()}/${AppPaths.appName}.db",
            properties = Properties().apply { put("foreign_keys", "true") },
        )
        AppDB.Schema.create(driver)
        AppDB(driver = driver)
    }

    val tradesDB: TradesDB = run {
        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:${AppPaths.getAppDataPath()}/Trades.db",
            properties = Properties().apply { put("foreign_keys", "true") },
        )
        TradesDB.Schema.create(driver)
        TradesDB(
            driver = driver,
            TradeAdapter = Trade.Adapter(
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

    val appPrefs = PreferencesSettings(Preferences.userRoot().node(AppPaths.appName)).toFlowSettings()

    val fyersApi by lazy { FyersApi() }

    val candleDBCollection = CandleDBCollection()

    val candleRepo = CandleRepository(
        candleDownloader = FyersCandleDownloader(this),
        candleCache = CandleCacheDB(this),
    )

    val tradeOrdersRepo by lazy { TradeOrdersRepo(tradesDB) }

    val tradesRepo by lazy { TradesRepo(tradesDB, tradeOrdersRepo) }

    init {
//        TradeImporter(appDB).importTrades()
//        TradeMigrator(tradesRepo, tradeOrdersRepo, appDB, tradesDB).migrateTrades()
    }
}
