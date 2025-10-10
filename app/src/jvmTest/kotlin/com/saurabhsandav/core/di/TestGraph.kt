package com.saurabhsandav.core.di

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.backup.BackupManager
import com.saurabhsandav.core.trading.SymbolsProvider
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.AppPaths
import com.saurabhsandav.fyersapi.FyersApi
import com.saurabhsandav.trading.broker.BrokerProvider
import com.saurabhsandav.trading.candledata.CandleRepository
import com.saurabhsandav.trading.test.TestBrokerProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import java.nio.file.FileSystem
import kotlin.coroutines.CoroutineContext

@DependencyGraph(AppScope::class, bindingContainers = [CommonBindings::class])
internal interface TestGraph {

    val fileSystem: FileSystem

    val appPaths: AppPaths

    val appDB: AppDB

    val backupManager: BackupManager

    val tradingProfiles: TradingProfiles

    val symbolsProvider: SymbolsProvider

    @AppCoroutineScope
    @Binds
    fun provideAppScope(testScope: TestScope): CoroutineScope

    @SingleIn(AppScope::class)
    @Provides
    fun provideAppDispatchers(testScope: TestScope): AppDispatchers {

        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

        return AppDispatchers(
            Main = testDispatcher,
            Default = testDispatcher,
            IO = testDispatcher,
            Unconfined = testDispatcher,
        )
    }

    @SingleIn(AppScope::class)
    @Provides
    fun provideFileSystem(): FileSystem = Jimfs.newFileSystem(Configuration.unix())

    @SingleIn(AppScope::class)
    @Provides
    fun provideAppDB(appPaths: AppPaths): AppDB = run {

        val driver = JdbcSqliteDriver(
            url = JdbcSqliteDriver.IN_MEMORY,
            schema = AppDB.Schema,
        )

        AppDB(driver)
    }

    @Provides
    fun provideBrokerProvider(): BrokerProvider = TestBrokerProvider

    @SingleIn(AppScope::class)
    @Provides
    fun provideFyersApi(): FyersApi = TODO("Replace dependents with Fakes")

    @SingleIn(AppScope::class)
    @Provides
    fun provideCandleRepo(
        @AppCoroutineScope appScope: CoroutineScope,
        @IOCoroutineContext ioCoroutineContext: CoroutineContext,
        @AppPrefs appPrefs: FlowSettings,
        fyersApi: FyersApi,
    ): CandleRepository = TODO("Replace with Fakes")

    @DependencyGraph.Factory
    fun interface Factory {

        fun create(
            @Provides testScope: TestScope,
        ): TestGraph
    }
}
