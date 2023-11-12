package com.saurabhsandav.core

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import com.saurabhsandav.core.fyers_api.FyersApi
import com.saurabhsandav.core.trades.TradeManagementJob
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.Account
import com.saurabhsandav.core.trades.model.ProfileIdColumnAdapter
import com.saurabhsandav.core.trading.data.*
import com.saurabhsandav.core.trading.data.db.CandleQueriesCollection
import com.saurabhsandav.core.ui.common.webview.CefWebViewState
import com.saurabhsandav.core.ui.common.webview.JavaFxWebViewState
import com.saurabhsandav.core.ui.loginservice.LoginServicesManager
import com.saurabhsandav.core.ui.settings.model.WebViewBackend
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradeexecutions.TradeExecutionsModule
import com.saurabhsandav.core.ui.trades.TradesModule
import com.saurabhsandav.core.utils.AppPaths
import com.saurabhsandav.core.utils.InstantColumnAdapter
import com.saurabhsandav.core.utils.PrefKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE
import java.util.*
import java.util.prefs.Preferences
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream

internal class AppModule {

    val appScope = MainScope()

    init {

        setupLogging()
    }

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
            TradingProfileAdapter = TradingProfile.Adapter(
                idAdapter = ProfileIdColumnAdapter,
            ),
        )
    }

    private val candleDBDriver: SqlDriver = run {
        JdbcSqliteDriver(
            url = "jdbc:sqlite:${AppPaths.getAppDataPath()}/Candles.db",
            properties = Properties().apply { put("foreign_keys", "true") },
        )
    }

    private val candleDB: CandleDB = run {
        CandleDB.Schema.create(candleDBDriver)
        CandleDB(
            driver = candleDBDriver,
            CheckedRangeAdapter = CheckedRange.Adapter(
                fromEpochSecondsAdapter = InstantColumnAdapter,
                toEpochSecondsAdapter = InstantColumnAdapter,
            )
        )
    }

    private val _appPrefs = PreferencesSettings(Preferences.userRoot().node(AppPaths.appName))

    val appPrefs = _appPrefs.toFlowSettings()

    val webViewStateProvider = run {

        val webViewBackend = _appPrefs.getString(PrefKeys.WebViewBackend, WebViewBackend.JCEF.name);

        {
            when (webViewBackend) {
                WebViewBackend.JCEF.name -> CefWebViewState()
                WebViewBackend.JavaFX.name -> JavaFxWebViewState()
                else -> error("Invalid WebView Backend: $webViewBackend")
            }
        }
    }

    val loginServicesManager by lazy { LoginServicesManager() }

    val fyersApi by lazy { FyersApi() }

    private val candleQueriesCollection = CandleQueriesCollection(driver = candleDBDriver)

    val candleRepo = CandleRepository(
        candleDownloader = FyersCandleDownloader(this),
        candleCache = CandleCacheDB(
            candleDB = candleDB,
            candleQueriesCollection = candleQueriesCollection,
        ),
    )

    val tradingProfiles = TradingProfiles(
        appFilesPath = AppPaths.getAppDataPath(),
        appDB = appDB,
        candleRepo = candleRepo,
    )

    val tradeExecutionsModule = {
            coroutineScope: CoroutineScope,
            tradeContentLauncher: TradeContentLauncher,
        ->
        TradeExecutionsModule(this, coroutineScope, tradeContentLauncher)
    }

    val tradesModule = {
            coroutineScope: CoroutineScope,
            tradeContentLauncher: TradeContentLauncher,
        ->
        TradesModule(this, coroutineScope, tradeContentLauncher)
    }

    init {

        runStartupJobs()
    }

    private fun setupLogging() {

        // Current time
        val currentTime = Clock.System.now().epochSeconds

        // Log file path
        val logDirectory = Path(AppPaths.getAppDataPath(), "logs")
        val logFile = logDirectory.resolve("$currentTime.log")

        // Create log directory
        logDirectory.createDirectories()

        // Create FileLogWriter
        val fileLogWriter = object : LogWriter() {

            private val writer by lazy {
                logFile.outputStream(CREATE, APPEND).bufferedWriter()
            }

            override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {

                appScope.launch(Dispatchers.IO) {

                    writer.write("$severity: ($tag) $message\n")

                    throwable?.let {
                        writer.write("${it.stackTraceToString()}\n")
                    }

                    writer.flush()
                }
            }
        }

        // Set FileLogWriter as the only LogWriter
        Logger.addLogWriter(fileLogWriter)

        val globalExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            Logger.e(e) { "Unhandled exception caught!" }
        }

        Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler)
    }

    private fun runStartupJobs() {

        val startupJobs = listOf(
            TradeManagementJob(
                tradingProfiles = tradingProfiles,
                candleRepo = candleRepo,
            ),
        )

        startupJobs.forEach { job ->
            appScope.launch { job.run() }
        }
    }
}
