package com.saurabhsandav.core

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import com.saurabhsandav.core.fyers_api.FyersApi
import com.saurabhsandav.core.trades.TradeManagementJob
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.Account
import com.saurabhsandav.core.trading.data.CandleCacheDB
import com.saurabhsandav.core.trading.data.CandleDBCollection
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.trading.data.FyersCandleDownloader
import com.saurabhsandav.core.ui.common.webview.JavaFxWebView
import com.saurabhsandav.core.ui.loginservice.LoginServicesManager
import com.saurabhsandav.core.utils.AppPaths
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
        AppDB(
            driver = driver,
            ClosedTradeAdapter = ClosedTrade.Adapter(IntColumnAdapter),
            OpenTradeAdapter = OpenTrade.Adapter(IntColumnAdapter),
        )
    }

    val appPrefs = PreferencesSettings(Preferences.userRoot().node(AppPaths.appName)).toFlowSettings()

    val webViewProvider = { JavaFxWebView() }

    val loginServicesManager by lazy { LoginServicesManager() }

    val fyersApi by lazy { FyersApi() }

    val candleDBCollection = CandleDBCollection()

    val candleRepo = CandleRepository(
        candleDownloader = FyersCandleDownloader(this),
        candleCache = CandleCacheDB(this),
    )

    val tradingProfiles = TradingProfiles(
        appFilesPath = AppPaths.getAppDataPath(),
        appDB = appDB,
        appPrefs = appPrefs,
        candleRepo = candleRepo,
    )

    init {

        runStartupJobs()
    }

    private fun runStartupJobs() {

        val startupScope = MainScope()

        val startupJobs = listOf(
            TradeManagementJob(tradingProfiles),
        )

        startupJobs.forEach { job ->
            startupScope.launch { job.run() }
        }
    }
}
