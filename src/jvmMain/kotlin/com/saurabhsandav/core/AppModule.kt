package com.saurabhsandav.core

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import com.saurabhsandav.core.fyers_api.FyersApi
import com.saurabhsandav.core.trades.model.Account
import com.saurabhsandav.core.trading.data.CandleDBCollection
import com.saurabhsandav.core.utils.AppPaths
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

    val appPrefs = PreferencesSettings(Preferences.userRoot().node(AppPaths.appName)).toFlowSettings()

    val fyersApi by lazy { FyersApi() }

    val candleDBCollection = CandleDBCollection()

    init {
//        TradeImporter(this).importTrades()
//        TradeMigrator(this).migrateTrades()
    }
}
