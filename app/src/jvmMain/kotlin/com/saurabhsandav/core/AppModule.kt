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
import com.saurabhsandav.core.ui.attachmentform.AttachmentFormModule
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
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.ui.stockchart.StockChartsStateFactory
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
import com.saurabhsandav.core.utils.*
import com.saurabhsandav.fyers_api.FyersApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okio.Path.Companion.toOkioPath
import java.util.*

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

    private val dbUrlProvider = DbUrlProvider(appPaths)

    val appDB: AppDB = run {

        val driver = JdbcSqliteDriver(
            url = dbUrlProvider.getAppDbUrl(),
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
            url = dbUrlProvider.getCandlesDbUrl(),
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
        dbUrlProvider = dbUrlProvider,
        appDB = appDB,
    )

    val tradeExcursionsGenerator = TradeExcursionsGenerator(
        appDispatchers = appDispatchers,
        tradingProfiles = tradingProfiles,
        candleRepo = candleRepo,
    )

    val tradeContentLauncher = TradeContentLauncher()

    val accountModule: (CoroutineScope) -> AccountModule = { coroutineScope ->
        AccountModule(this, coroutineScope)
    }

    val barReplayModule: (CoroutineScope) -> BarReplayModule = { coroutineScope ->
        BarReplayModule(this, coroutineScope)
    }

    val chartsModule: (CoroutineScope) -> ChartsModule = { coroutineScope ->
        ChartsModule(this, coroutineScope)
    }

    val tradeReviewModule: (CoroutineScope) -> TradeReviewModule = { coroutineScope ->
        TradeReviewModule(this, coroutineScope)
    }

    val landingModule: (CoroutineScope, ProfileId) -> LandingModule = { coroutineScope, profileId ->
        LandingModule(this, coroutineScope, profileId)
    }

    val profilesModule: (CoroutineScope) -> ProfilesModule = { coroutineScope ->
        ProfilesModule(this, coroutineScope)
    }

    val profileFormModule: (CoroutineScope) -> ProfileFormModule = { coroutineScope ->
        ProfileFormModule(this, coroutineScope)
    }

    val reviewsModule: (CoroutineScope, ProfileId) -> ReviewsModule = { coroutineScope, profileId ->
        ReviewsModule(this, coroutineScope, profileId)
    }

    val reviewModule: (CoroutineScope, ProfileReviewId) -> ReviewModule = { coroutineScope, profileReviewId ->
        ReviewModule(this, coroutineScope, profileReviewId)
    }

    val settingsModule: (CoroutineScope) -> SettingsModule = { coroutineScope ->
        SettingsModule(this, coroutineScope)
    }

    val sizingModule: (CoroutineScope, ProfileId) -> SizingModule = { coroutineScope, profileId ->
        SizingModule(this, coroutineScope, profileId)
    }

    val statsModule: (CoroutineScope, ProfileId) -> StatsModule = { coroutineScope, profileId ->
        StatsModule(this, coroutineScope, profileId)
    }

    val tagsModule: (CoroutineScope, ProfileId) -> TagsModule = { coroutineScope, profileId ->
        TagsModule(this, coroutineScope, profileId)
    }

    val tagFormModule: (CoroutineScope) -> TagFormModule = { coroutineScope ->
        TagFormModule(this, coroutineScope)
    }

    val tradeModule: (CoroutineScope) -> TradeModule = { coroutineScope ->
        TradeModule(this, coroutineScope)
    }

    val tradeExecutionFormModule: (CoroutineScope) -> TradeExecutionFormModule = { coroutineScope ->
        TradeExecutionFormModule(this, coroutineScope)
    }

    val tradeExecutionsModule: (CoroutineScope, ProfileId) -> TradeExecutionsModule = { coroutineScope, profileId ->
        TradeExecutionsModule(this, coroutineScope, profileId)
    }

    val tradesModule: (CoroutineScope, ProfileId) -> TradesModule = { coroutineScope, profileId ->
        TradesModule(this, coroutineScope, profileId)
    }

    val tradesFilterModule: (CoroutineScope, ProfileId) -> TradesFilterModule = { coroutineScope, profileId ->
        TradesFilterModule(this, coroutineScope, profileId)
    }

    val attachmentFormModule: (CoroutineScope) -> AttachmentFormModule = { coroutineScope ->
        AttachmentFormModule(this, coroutineScope)
    }

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
