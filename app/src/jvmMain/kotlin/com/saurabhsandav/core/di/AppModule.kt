package com.saurabhsandav.core.di

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import com.russhwolf.settings.datastore.DataStoreSettings
import com.saurabhsandav.core.AppConfig
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.CandleDB
import com.saurabhsandav.core.FileLogWriter
import com.saurabhsandav.core.backup.BackupManager
import com.saurabhsandav.core.backup.RestoreScheduler
import com.saurabhsandav.core.trading.data.CandleCacheDB
import com.saurabhsandav.core.trading.data.CandleDB
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.trading.data.FyersCandleDownloader
import com.saurabhsandav.core.trading.data.db.CandleQueriesCollection
import com.saurabhsandav.core.trading.record.TradeExcursionsGenerator
import com.saurabhsandav.core.trading.record.TradeManagementJob
import com.saurabhsandav.core.trading.record.TradingProfiles
import com.saurabhsandav.core.trading.record.model.Account
import com.saurabhsandav.core.ui.common.webview.CefWebViewState
import com.saurabhsandav.core.ui.common.webview.MyCefApp
import com.saurabhsandav.core.ui.loginservice.LoginServicesManager
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.ui.stockchart.StockChartsStateFactory
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.AppPaths
import com.saurabhsandav.core.utils.AppUriHandler
import com.saurabhsandav.core.utils.DbUrlProvider
import com.saurabhsandav.fyersapi.FyersApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import okio.Path.Companion.toOkioPath
import java.util.Properties

internal class AppModule(
    isDebugMode: Boolean,
    val restoreScheduler: RestoreScheduler,
) {

    val appScope = MainScope()

    private val appPaths = AppPaths(isDebugMode)

    val appDispatchers = AppDispatchers()

    val backupManager = BackupManager(
        appPaths = appPaths,
        appDispatchers = appDispatchers,
    )

    private val fileLogWriter = FileLogWriter(
        appDispatchers = appDispatchers,
        coroutineScope = appScope,
        appPaths = appPaths,
    )

    val uriHandler = AppUriHandler()

    init {

        setupLogging()
    }

    val account: Flow<Account> = flowOf(
        Account(
            balance = 11250.toBigDecimal(),
            balancePerTrade = 11250.toBigDecimal(),
            leverage = 5.toBigDecimal(),
            riskAmount = 11250.toBigDecimal() * 0.02.toBigDecimal(),
        ),
    )

    private val dbUrlProvider = DbUrlProvider(appPaths)

    val appDB: AppDB = run {

        val driver = JdbcSqliteDriver(
            url = dbUrlProvider.getAppDbUrl(),
            properties = Properties().apply { put("foreign_keys", "true") },
            schema = AppDB.Schema,
        )

        AppDB(driver)
    }

    private val candleDBDriver: SqlDriver = run {
        JdbcSqliteDriver(
            url = dbUrlProvider.getCandlesDbUrl(),
            properties = Properties().apply { put("foreign_keys", "true") },
            schema = CandleDB.Schema,
        )
    }

    private val candleDB: CandleDB = CandleDB(candleDBDriver)

    val appPrefs = DataStoreSettings(
        datastore = PreferenceDataStoreFactory.createWithPath(
            scope = appScope + appDispatchers.IO,
            corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() }),
            produceFile = { appPaths.prefsPath.resolve("app.preferences_pb").toOkioPath() },
        ),
    )

    val appConfig = AppConfig(
        scope = appScope,
        appPrefs = appPrefs,
    )

    private val chartPrefs by lazy {

        DataStoreSettings(
            datastore = PreferenceDataStoreFactory.createWithPath(
                scope = appScope + appDispatchers.IO,
                corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() }),
                produceFile = { appPaths.prefsPath.resolve("stockcharts.preferences_pb").toOkioPath() },
            ),
        )
    }

    val myCefApp = lazy { MyCefApp(appPaths) }

    val webViewStateProvider = { coroutineScope: CoroutineScope ->
        CefWebViewState(coroutineScope, appDispatchers, myCefApp.value)
    }

    val loginServicesManager by lazy { LoginServicesManager() }

    val fyersApi by lazy { FyersApi() }

    private val candleQueriesCollection = CandleQueriesCollection(driver = candleDBDriver)

    val candleRepo = CandleRepository(
        candleDownloader = FyersCandleDownloader(
            coroutineScope = appScope,
            appPrefs = appPrefs,
            fyersApi = fyersApi,
        ),
        candleCache = CandleCacheDB(
            coroutineContext = appDispatchers.IO,
            candleDB = candleDB,
            candleQueriesCollection = candleQueriesCollection,
        ),
    )

    val tradingProfiles = TradingProfiles(
        coroutineContext = appDispatchers.IO,
        appPaths = appPaths,
        dbUrlProvider = dbUrlProvider,
        appDB = appDB,
    )

    val tradeExcursionsGenerator = TradeExcursionsGenerator(
        coroutineContext = appDispatchers.IO,
        tradingProfiles = tradingProfiles,
        candleRepo = candleRepo,
    )

    val tradeContentLauncher = TradeContentLauncher()

    val stockChartsState = StockChartsStateFactory {
        coroutineScope,
        initialParams,
        loadConfig,
        marketDataProvider,
        ->

        StockChartsState(
            parentScope = coroutineScope,
            initialParams = initialParams,
            marketDataProvider = marketDataProvider,
            appPrefs = appPrefs,
            chartPrefs = chartPrefs,
            webViewStateProvider = webViewStateProvider,
            loadConfig = loadConfig,
        )
    }

    val screensModule = ScreensModule(this)

    init {

        runStartupJobs()
    }

    private fun setupLogging() {

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
                excursionsGenerator = tradeExcursionsGenerator,
            ),
        )

        startupJobs.forEach { job ->
            appScope.launch { job.run() }
        }
    }

    fun destroy() {
        fileLogWriter.destroy()
        if (myCefApp.isInitialized()) myCefApp.value.dispose()
        appScope.cancel()
    }
}
