package com.saurabhsandav.core.trading

import com.saurabhsandav.core.di.TestGraph
import com.saurabhsandav.trading.test.TestBroker
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class AppSymbolsProviderTest {

    @Test
    fun `downloadAllLatestSymbols - Download first time`() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)

        // Database is empty
        assertEquals(emptyList(), testGraph.getSymbolIdsFromDB())

        // Download symbols
        launch {
            testGraph.symbolsProvider.downloadAllLatestSymbols()
        }

        // Skip expiry check logic
        advanceTimeBy(10.milliseconds)

        // First symbols load
        advanceTimeBy(TestBroker.SymbolFetchDelay)
        assertEquals(TestBroker.TestSymbols.slice(0..0).map { it.id }, testGraph.getSymbolIdsFromDB())

        // Last symbols load
        advanceTimeBy(TestBroker.SymbolFetchDelay)
        assertEquals(TestBroker.TestSymbols.map { it.id }, testGraph.getSymbolIdsFromDB())
    }

    @Test
    fun `downloadAllLatestSymbols - Download if symbols expired`() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)

        // Expire symbols
        testGraph.appDB.symbolDownloadTimestampQueries.put(
            brokerId = TestBroker.Id,
            timestamp = Clock.System.now() - (TestBroker.SymbolExpiryPeriod * 2),
        )

        // Attempt redownloading symbols in parallel
        val job = launch {
            testGraph.symbolsProvider.downloadAllLatestSymbols()
        }

        // Skip expiry check logic
        advanceTimeBy(10.milliseconds)

        // All symbols loaded
        assertFalse { job.isCompleted }
        advanceTimeBy(TestBroker.SymbolFetchDelay * 2)
        assertEquals(TestBroker.TestSymbols.map { it.id }, testGraph.getSymbolIdsFromDB())
    }

    @Test
    fun `downloadAllLatestSymbols - Skip if Symbols not expired`() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)

        // Pre download symbols to test database clearing
        testGraph.symbolsProvider.downloadAllLatestSymbols()

        // Attempt redownloading symbols in parallel
        val job = launch {
            testGraph.symbolsProvider.downloadAllLatestSymbols()
        }

        // Skip expiry check logic
        advanceTimeBy(10.milliseconds)

        assertTrue { job.isCompleted }
    }

    private fun TestGraph.getSymbolIdsFromDB() = appDB.cachedSymbolQueries.getAll().executeAsList().map { it.id }
}
