package com.saurabhsandav.core.di

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import com.saurabhsandav.core.AppConfig
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.StartupManager
import com.saurabhsandav.core.backup.BackupManager
import com.saurabhsandav.core.backup.RestoreScheduler
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.trading.data.FyersCandleDownloader
import com.saurabhsandav.core.ui.attachmentform.AttachmentFormGraph
import com.saurabhsandav.core.ui.barreplay.BarReplayGraph
import com.saurabhsandav.core.ui.charts.ChartsGraph
import com.saurabhsandav.core.ui.common.webview.CefWebViewState
import com.saurabhsandav.core.ui.common.webview.MyCefApp
import com.saurabhsandav.core.ui.common.webview.WebViewState
import com.saurabhsandav.core.ui.landing.LandingGraph
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorGraph
import com.saurabhsandav.core.ui.profiles.ProfilesGraph
import com.saurabhsandav.core.ui.profiles.form.ProfileFormGraph
import com.saurabhsandav.core.ui.review.ReviewGraph
import com.saurabhsandav.core.ui.settings.SettingsGraph
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.ui.symbolselectiondialog.SymbolSelectionGraph
import com.saurabhsandav.core.ui.tags.form.TagFormGraph
import com.saurabhsandav.core.ui.tags.selector.TagSelectorState
import com.saurabhsandav.core.ui.trade.TradeGraph
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradeexecutionform.TradeExecutionFormGraph
import com.saurabhsandav.core.ui.tradereview.TradeReviewGraph
import com.saurabhsandav.core.ui.tradesfiltersheet.TradesFilterGraph
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.AppPaths
import com.saurabhsandav.fyersapi.FyersApi
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.candledata.CandleCacheDB
import com.saurabhsandav.trading.candledata.CandleRepository
import com.saurabhsandav.trading.record.model.Account
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.plus
import okio.Path.Companion.toOkioPath
import java.util.Properties
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.absolutePathString

@DependencyGraph(AppScope::class)
internal interface AppGraph {

    val tradingProfiles: TradingProfiles

    val backupManager: BackupManager

    val appConfig: AppConfig

    val tradeContentLauncher: TradeContentLauncher

    val startupManager: StartupManager

    val tagSelectorStateFactory: TagSelectorState.Factory

    // region GraphExtensions

    val landingGraphFactory: LandingGraph.Factory

    val attachmentFormGraphFactory: AttachmentFormGraph.Factory

    val barReplayGraphFactory: BarReplayGraph.Factory

    val chartsGraphFactory: ChartsGraph.Factory

    val pnlCalculatorGraphFactory: PNLCalculatorGraph.Factory

    val profilesGraphFactory: ProfilesGraph.Factory

    val profileFormGraphFactory: ProfileFormGraph.Factory

    val reviewGraphFactory: ReviewGraph.Factory

    val settingsGraphFactory: SettingsGraph.Factory

    val symbolSelectionGraphFactory: SymbolSelectionGraph.Factory

    val tagFormGraphFactory: TagFormGraph.Factory

    val tradeGraphFactory: TradeGraph.Factory

    val tradeExecutionFormGraphFactory: TradeExecutionFormGraph.Factory

    val tradeReviewGraphFactory: TradeReviewGraph.Factory

    val tradesFilterGraphFactory: TradesFilterGraph.Factory

    // endregion GraphExtensions

    @SingleIn(AppScope::class)
    @AppCoroutineScope
    @Provides
    fun provideAppScope(): CoroutineScope = MainScope()

    @IOCoroutineContext
    @Provides
    fun provideIOCoroutineContext(appDispatchers: AppDispatchers): CoroutineContext = appDispatchers.IO

    @SingleIn(AppScope::class)
    @Provides
    fun provideAccount(): Flow<Account> = flowOf(
        Account(
            balance = 11250.toKBigDecimal(),
            balancePerTrade = 11250.toKBigDecimal(),
            leverage = 5.toKBigDecimal(),
            riskAmount = 11250.toKBigDecimal() * 0.02.toKBigDecimal(),
        ),
    )

    @SingleIn(AppScope::class)
    @Provides
    fun provideAppDB(appPaths: AppPaths): AppDB = run {

        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:${appPaths.appDBPath.absolutePathString()}",
            properties = Properties().apply { put("foreign_keys", "true") },
            schema = AppDB.Schema,
        )

        AppDB(driver)
    }

    @SingleIn(AppScope::class)
    @AppPrefs
    @Provides
    fun provideAppPrefs(
        @AppCoroutineScope appScope: CoroutineScope,
        @IOCoroutineContext ioCoroutineContext: CoroutineContext,
        appPaths: AppPaths,
    ): FlowSettings = DataStoreSettings(
        datastore = PreferenceDataStoreFactory.createWithPath(
            scope = appScope + ioCoroutineContext,
            corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() }),
            produceFile = { appPaths.prefsPath.resolve("app.preferences_pb").toOkioPath() },
        ),
    )

    @SingleIn(AppScope::class)
    @ChartPrefs
    @Provides
    fun provideChartPrefs(
        @AppCoroutineScope appScope: CoroutineScope,
        @IOCoroutineContext ioCoroutineContext: CoroutineContext,
        appPaths: AppPaths,
    ): FlowSettings = DataStoreSettings(
        datastore = PreferenceDataStoreFactory.createWithPath(
            scope = appScope + ioCoroutineContext,
            corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() }),
            produceFile = { appPaths.prefsPath.resolve("stockcharts.preferences_pb").toOkioPath() },
        ),
    )

    @SingleIn(AppScope::class)
    @Provides
    fun provideWebViewStateFactory(
        appDispatchers: AppDispatchers,
        myCefApp: MyCefApp,
    ): WebViewState.Factory = WebViewState.Factory { coroutineScope ->
        CefWebViewState(
            coroutineScope = coroutineScope,
            appDispatchers = appDispatchers,
            myCefApp = myCefApp,
        )
    }

    @SingleIn(AppScope::class)
    @Provides
    fun provideFyersApi(): FyersApi = FyersApi()

    @SingleIn(AppScope::class)
    @Provides
    fun provideCandleRepo(
        @AppCoroutineScope appScope: CoroutineScope,
        @IOCoroutineContext ioCoroutineContext: CoroutineContext,
        @AppPrefs appPrefs: FlowSettings,
        fyersApi: FyersApi,
    ): CandleRepository = CandleRepository(
        candleDownloader = FyersCandleDownloader(
            coroutineScope = appScope,
            appPrefs = appPrefs,
            fyersApi = fyersApi,
        ),
        candleCache = CandleCacheDB(
            coroutineContext = ioCoroutineContext,
        ),
    )

    @SingleIn(AppScope::class)
    @Provides
    fun provideStockChartsStateFactory(
        @AppPrefs appPrefs: FlowSettings,
        @ChartPrefs chartPrefs: FlowSettings,
        webViewStateFactory: WebViewState.Factory,
    ): StockChartsState.Factory = StockChartsState.Factory {
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
            webViewStateFactory = webViewStateFactory,
            loadConfig = loadConfig,
        )
    }

    @DependencyGraph.Factory
    fun interface Factory {

        fun create(
            @IsDebugMode @Provides isDebugMode: Boolean,
            @Provides restoreScheduler: RestoreScheduler,
        ): AppGraph
    }
}
