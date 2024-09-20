package com.saurabhsandav.core

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import com.russhwolf.settings.datastore.DataStoreSettings
import com.saurabhsandav.core.trades.TradeExcursionsGenerator
import com.saurabhsandav.core.trades.TradeManagementJob
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.Account
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.ProfileIdColumnAdapter
import com.saurabhsandav.core.trading.data.*
import com.saurabhsandav.core.trading.data.db.CandleQueriesCollection
import com.saurabhsandav.core.ui.account.AccountModule
import com.saurabhsandav.core.ui.barreplay.BarReplayModule
import com.saurabhsandav.core.ui.charts.ChartsModule
import com.saurabhsandav.core.ui.common.webview.CefWebViewState
import com.saurabhsandav.core.ui.common.webview.JavaFxWebViewState
import com.saurabhsandav.core.ui.common.webview.MyCefApp
import com.saurabhsandav.core.ui.landing.LandingModule
import com.saurabhsandav.core.ui.loginservice.LoginServicesManager
import com.saurabhsandav.core.ui.profiles.ProfilesModule
import com.saurabhsandav.core.ui.profiles.form.ProfileFormModule
import com.saurabhsandav.core.ui.review.ReviewModule
import com.saurabhsandav.core.ui.reviews.ReviewsModule
import com.saurabhsandav.core.ui.settings.SettingsModule
import com.saurabhsandav.core.ui.settings.model.WebViewBackend
import com.saurabhsandav.core.ui.sizing.SizingModule
import com.saurabhsandav.core.ui.stats.StatsModule
import com.saurabhsandav.core.ui.stockchart.*
import com.saurabhsandav.core.ui.tagform.TagFormModule
import com.saurabhsandav.core.ui.tags.TagsModule
import com.saurabhsandav.core.ui.trade.TradeModule
import com.saurabhsandav.core.ui.tradecontent.ProfileReviewId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradeexecutionform.TradeExecutionFormModule
import com.saurabhsandav.core.ui.tradeexecutions.TradeExecutionsModule
import com.saurabhsandav.core.ui.tradereview.TradeReviewModule
import com.saurabhsandav.core.ui.trades.TradesModule
import com.saurabhsandav.core.ui.tradesfiltersheet.TradesFilterModule
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.AppPaths
import com.saurabhsandav.core.utils.InstantColumnAdapter
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.fyers_api.FyersApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okio.Path.Companion.toOkioPath
import java.util.*
import kotlin.io.path.absolutePathString

internal class AppModule {

    val appScope = MainScope()

    private val appPaths = AppPaths()

    val appDispatchers = AppDispatchers()

    private val fileLogWriter = FileLogWriter(
        appDispatchers = appDispatchers,
        coroutineScope = appScope,
        appPaths = appPaths,
    )

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
            url = "jdbc:sqlite:${appPaths.appDataPath.absolutePathString()}/${appPaths.appName}.db",
            properties = Properties().apply { put("foreign_keys", "true") },
            schema = AppDB.Schema,
        )

        AppDB(
            driver = driver,
            TradingProfileAdapter = TradingProfile.Adapter(
                idAdapter = ProfileIdColumnAdapter,
                tradeCountAdapter = IntColumnAdapter,
                tradeCountOpenAdapter = IntColumnAdapter,
            ),
        )
    }

    private val candleDBDriver: SqlDriver = run {
        JdbcSqliteDriver(
            url = "jdbc:sqlite:${appPaths.appDataPath.absolutePathString()}/Candles.db",
            properties = Properties().apply { put("foreign_keys", "true") },
            schema = CandleDB.Schema,
        )
    }

    private val candleDB: CandleDB = run {
        CandleDB(
            driver = candleDBDriver,
            CheckedRangeAdapter = CheckedRange.Adapter(
                fromEpochSecondsAdapter = InstantColumnAdapter,
                toEpochSecondsAdapter = InstantColumnAdapter,
            )
        )
    }

    val appPrefs = DataStoreSettings(
        datastore = PreferenceDataStoreFactory.createWithPath {
            appPaths.prefsPath.resolve("app.preferences_pb").toOkioPath()
        },
    )

    private val myCefApp = lazy { MyCefApp(appPaths) }

    private val chartPrefs by lazy {

        DataStoreSettings(
            datastore = PreferenceDataStoreFactory.createWithPath {
                appPaths.prefsPath.resolve("stockcharts.preferences_pb").toOkioPath()
            },
        )
    }

    val webViewStateProvider = run {

        val webViewBackend = runBlocking {
            appPrefs.getString(PrefKeys.WebViewBackend, WebViewBackend.JCEF.name)
        };

        {
            when (webViewBackend) {
                WebViewBackend.JCEF.name -> CefWebViewState(appDispatchers, myCefApp.value)
                WebViewBackend.JavaFX.name -> JavaFxWebViewState()
                else -> error("Invalid WebView Backend: $webViewBackend")
            }
        }
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
            appDispatchers = appDispatchers,
            candleDB = candleDB,
            candleQueriesCollection = candleQueriesCollection,
        ),
    )

    val tradingProfiles = TradingProfiles(
        appDispatchers = appDispatchers,
        appFilesPath = appPaths.appDataPath,
        appDB = appDB,
    )

    val tradeExcursionsGenerator = TradeExcursionsGenerator(
        appDispatchers = appDispatchers,
        tradingProfiles = tradingProfiles,
        candleRepo = candleRepo,
    )

    val tradeContentLauncher = TradeContentLauncher()

    val accountModule = { coroutineScope: CoroutineScope ->
        AccountModule(this, coroutineScope)
    }

    val barReplayModule = { coroutineScope: CoroutineScope ->
        BarReplayModule(this, coroutineScope)
    }

    val chartsModule = { coroutineScope: CoroutineScope ->
        ChartsModule(this, coroutineScope)
    }

    val tradeReviewModule = { coroutineScope: CoroutineScope ->
        TradeReviewModule(this, coroutineScope)
    }

    val landingModule = {
            coroutineScope: CoroutineScope,
            profileId: ProfileId,
        ->
        LandingModule(this, coroutineScope, profileId)
    }

    val profilesModule = { coroutineScope: CoroutineScope ->
        ProfilesModule(this, coroutineScope)
    }

    val profileFormModule = { coroutineScope: CoroutineScope ->
        ProfileFormModule(this, coroutineScope)
    }

    val reviewsModule = {
            coroutineScope: CoroutineScope,
            profileId: ProfileId,
        ->
        ReviewsModule(this, coroutineScope, profileId)
    }

    val reviewModule = {
            coroutineScope: CoroutineScope,
            profileReviewId: ProfileReviewId,
        ->
        ReviewModule(this, coroutineScope, profileReviewId)
    }

    val settingsModule = { coroutineScope: CoroutineScope ->
        SettingsModule(this, coroutineScope)
    }

    val sizingModule = {
            coroutineScope: CoroutineScope,
            profileId: ProfileId,
        ->
        SizingModule(this, coroutineScope, profileId)
    }

    val statsModule = {
            coroutineScope: CoroutineScope,
            profileId: ProfileId,
        ->
        StatsModule(this, coroutineScope, profileId)
    }

    val tagsModule = {
            coroutineScope: CoroutineScope,
            profileId: ProfileId,
        ->
        TagsModule(this, coroutineScope, profileId)
    }

    val tagFormModule = { coroutineScope: CoroutineScope ->
        TagFormModule(this, coroutineScope)
    }

    val tradeModule = { coroutineScope: CoroutineScope ->
        TradeModule(this, coroutineScope)
    }

    val tradeExecutionFormModule = { coroutineScope: CoroutineScope ->
        TradeExecutionFormModule(this, coroutineScope)
    }

    val tradeExecutionsModule = {
            coroutineScope: CoroutineScope,
            profileId: ProfileId,
        ->
        TradeExecutionsModule(this, coroutineScope, profileId)
    }

    val tradesModule = {
            coroutineScope: CoroutineScope,
            profileId: ProfileId,
        ->
        TradesModule(this, coroutineScope, profileId)
    }

    val tradesFilterModule = {
            coroutineScope: CoroutineScope,
            profileId: ProfileId,
        ->
        TradesFilterModule(this, coroutineScope, profileId)
    }

    val stockChartsState = StockChartsStateFactory {
            coroutineScope: CoroutineScope,
            initialParams: StockChartParams,
            loadConfig: LoadConfig,
            marketDataProvider: MarketDataProvider,
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
