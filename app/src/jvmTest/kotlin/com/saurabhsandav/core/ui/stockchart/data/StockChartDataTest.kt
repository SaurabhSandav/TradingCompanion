package com.saurabhsandav.core.ui.stockchart.data

import app.cash.turbine.test
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.barreplay.CandleUtils
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.data.StockChartData.LoadState
import com.saurabhsandav.core.utils.binarySearchByAsResult
import com.saurabhsandav.core.utils.indexOr
import com.saurabhsandav.core.utils.indexOrNaturalIndex
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class StockChartDataTest {

    @Test
    fun `No load`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val candleSource = FakeCandleSource(params)
        val data = StockChartData(candleSource, loadConfig, LoadedPages()) { }

        assertFalse { candleSource.isInitialized }
        assertFalse { candleSource.isDestroyed }
        assertEquals(0, data.candleSeries.size)
    }

    @Test
    fun `Load Initial`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val candleSource = FakeCandleSource(params)
        val data = StockChartData(candleSource, loadConfig, LoadedPages()) { }

        data.loadState.test {

            expectNoEvents()

            assertFalse { candleSource.isInitialized }

            data.loadInitial()

            assertEquals(LoadState.Loading, awaitItem())
            assertEquals(LoadState.Loaded, awaitItem())
            ensureAllEventsConsumed()

            assertTrue { candleSource.isInitialized }
            assertFalse { candleSource.isDestroyed }
            assertEquals(20, data.candleSeries.size)
            assertEquals(CandleUtils.m5Series[1456], data.candleSeries.first())
            assertEquals(CandleUtils.m5Series[1475], data.candleSeries.last())
        }
    }

    @Test
    fun `Load Initial multiple times`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val candleSource = FakeCandleSource(params)
        val data = StockChartData(candleSource, loadConfig, LoadedPages()) { }

        data.loadState.test {

            expectNoEvents()

            assertFalse { candleSource.isInitialized }

            data.loadInitial()

            assertEquals(LoadState.Loading, awaitItem())
            assertEquals(LoadState.Loaded, awaitItem())

            assertEquals(20, data.candleSeries.size)
            assertEquals(CandleUtils.m5Series[1456], data.candleSeries.first())
            assertEquals(CandleUtils.m5Series[1475], data.candleSeries.last())

            data.loadInitial()
            ensureAllEventsConsumed()

            assertEquals(20, data.candleSeries.size)
            assertEquals(CandleUtils.m5Series[1456], data.candleSeries.first())
            assertEquals(CandleUtils.m5Series[1475], data.candleSeries.last())
        }
    }

    @Test
    fun `Load Initial with existing interval`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
            maxCandleCount = 40,
        )

        val loadedPages = LoadedPages()
        val data1 = StockChartData(FakeCandleSource(params), loadConfig, loadedPages) { }

        data1.loadInitial()
        repeat(4) { data1.loadBefore() }

        assertEquals(40, data1.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1416], data1.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1455], data1.candleSeries.last())

        val data2 = StockChartData(FakeCandleSource(params), loadConfig, loadedPages) { }

        assertEquals(0, data2.candleSeries.size)
        data2.loadInitial()

        assertEquals(40, data2.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1416], data2.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1455], data2.candleSeries.last())
    }

    @Test
    fun `Load Before, Custom load count`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val data = StockChartData(FakeCandleSource(params), loadConfig, LoadedPages()) { }

        data.loadInitial()
        data.loadBefore(loadCount = 15)

        assertEquals(40, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1436], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1475], data.candleSeries.last())
    }

    @Test
    fun `Load After`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val data = StockChartData(FakeCandleSource(params), loadConfig, LoadedPages()) { }

        data.loadInitial()
        data.loadAfter()

        assertEquals(30, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1456], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1485], data.candleSeries.last())
    }

    @Test
    fun `Load After, Custom load count`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val data = StockChartData(FakeCandleSource(params), loadConfig, LoadedPages()) { }

        data.loadInitial()
        data.loadAfter(loadCount = 15)

        assertEquals(40, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1456], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1495], data.candleSeries.last())
    }

    @Test
    fun `Load After and then Before`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val data = StockChartData(FakeCandleSource(params), loadConfig, LoadedPages()) { }

        data.loadInitial()
        data.loadAfter()
        data.loadBefore()

        assertEquals(40, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1446], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1485], data.candleSeries.last())
    }

    @Test
    fun `Load latest`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val data = StockChartData(FakeCandleSource(params), loadConfig, LoadedPages()) { }

        data.loadInitial()
        data.loadAfter()
        data.loadBefore()
        data.loadLatest()

        assertEquals(40, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1446], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1485], data.candleSeries.last())
    }

    @Test
    fun `Load Interval inside already loaded interval`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val data = StockChartData(FakeCandleSource(params), loadConfig, LoadedPages()) { }

        data.loadInitial()
        data.loadInterval(
            CandleUtils.m5Series[1460].openInstant - 1.seconds..CandleUtils.m5Series[1470].openInstant + 1.seconds,
        )

        assertEquals(20, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1456], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1475], data.candleSeries.last())
    }

    @Test
    fun `Load Interval outside already loaded interval`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val data = StockChartData(FakeCandleSource(params), loadConfig, LoadedPages()) { }

        data.loadInitial()
        data.loadInterval(
            CandleUtils.m5Series[1230].openInstant - 1.seconds..CandleUtils.m5Series[1250].openInstant + 1.seconds,
        )

        assertEquals(41, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1220], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1260], data.candleSeries.last())
    }

    @Test
    fun `Load Interval and then load latest`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val data = StockChartData(FakeCandleSource(params), loadConfig, LoadedPages()) { }

        data.loadInitial()
        data.loadInterval(
            CandleUtils.m5Series[1230].openInstant - 1.seconds..CandleUtils.m5Series[1250].openInstant + 1.seconds,
        )
        data.loadLatest()

        assertEquals(20, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1456], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1475], data.candleSeries.last())
    }

    @Test
    fun `Initial candles updates with flow`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val flowBlock = CompletableDeferred<Unit>()

        val candleSource = FakeCandleSource(params) { candles ->
            flow {
                emit(candles.take(candles.size / 2))
                flowBlock.await()
                emit(candles)
            }
        }
        val data = StockChartData(candleSource, loadConfig, LoadedPages()) { }

        data.loadInitial()

        data.candleSeries.instantRange.test {
            awaitItem()
            assertEquals(10, data.candleSeries.size)
            assertEquals(CandleUtils.m5Series[1456], data.candleSeries.first())
            assertEquals(CandleUtils.m5Series[1465], data.candleSeries.last())

            flowBlock.complete(Unit)

            awaitItem()
            assertEquals(20, data.candleSeries.size)
            assertEquals(CandleUtils.m5Series[1456], data.candleSeries.first())
            assertEquals(CandleUtils.m5Series[1475], data.candleSeries.last())
        }
    }

    @Test
    fun `Live candles`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val candleSource = FakeCandleSource(params, emitLive = true)
        val data = StockChartData(candleSource, loadConfig, LoadedPages()) { }

        data.loadInitial()

        // Check initial load
        assertEquals(20, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1456], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1475], data.candleSeries.last())

        // Check 5 live candles
        data.candleSeries.live.test {
            for (i in 0 until 5) {
                assertEquals(IndexedValue(20 + i, CandleUtils.m5Series[1476 + i]), awaitItem())
            }
        }

        // Check loaded range after live candles
        assertEquals(25, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1456], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1480], data.candleSeries.last())
    }

    @Test
    fun `Destroy StockChartData`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val candleSource = FakeCandleSource(params, emitLive = true)
        val data = StockChartData(candleSource, loadConfig, LoadedPages()) { }

        data.loadInitial()

        // Check initial load
        assertEquals(20, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1456], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1475], data.candleSeries.last())

        data.candleSeries.live.test {

            // Skip some live candles
            repeat(5) { awaitItem() }

            // Destroy StockChartData
            data.destroy()
            ensureAllEventsConsumed()
            assertTrue { candleSource.isDestroyed }
        }
    }

    @Test
    fun `Load Before - Max Candle Count crossed`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
            maxCandleCount = 40,
        )

        val candleSource = FakeCandleSource(params)
        val data = StockChartData(candleSource, loadConfig, LoadedPages()) { }

        // 20 candles
        data.loadInitial()
        // 30 candles
        data.loadBefore()
        // 40 candles
        data.loadBefore()
        // 30 candles (Max candle count crossed, Initial page dropped)
        data.loadBefore()

        assertEquals(30, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1426], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1455], data.candleSeries.last())
    }

    @Test
    fun `Load After - Max Candle Count crossed`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
            maxCandleCount = 40,
        )

        val candleSource = FakeCandleSource(params)
        val data = StockChartData(candleSource, loadConfig, LoadedPages()) { }

        // 20 candles
        data.loadInitial()
        // 30 candles
        data.loadAfter()
        // 40 candles
        data.loadAfter()
        // 30 candles (Max candle count crossed, Initial page dropped)
        data.loadAfter()

        assertEquals(30, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1476], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1505], data.candleSeries.last())
    }

    @Test
    fun `Load before, after and before - Max Candle Count crossed`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
            maxCandleCount = 40,
        )

        val candleSource = FakeCandleSource(params)
        val data = StockChartData(candleSource, loadConfig, LoadedPages()) { }

        // 20 candles
        data.loadInitial()
        // 30 candles
        data.loadBefore()
        // 40 candles
        data.loadAfter()
        // 30 candles (Max candle count crossed, Initial page dropped)
        data.loadBefore()

        assertEquals(40, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1436], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1475], data.candleSeries.last())
    }

    @Test
    fun `onCandlesLoaded Callback`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
            maxCandleCount = 40,
        )

        val channel = Channel<Int>(Channel.BUFFERED)
        var loadCount = 0

        val candleSource = FakeCandleSource(params)
        val data = StockChartData(candleSource, loadConfig, LoadedPages()) { channel.trySend(loadCount++) }

        channel.consumeAsFlow().test {
            expectNoEvents()
            data.loadInitial()
            assertEquals(0, awaitItem())
            data.loadBefore()
            assertEquals(1, awaitItem())
        }
    }

    @Test
    fun `Reload without changing interval`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val loadedPages = LoadedPages()
        val data = StockChartData(FakeCandleSource(params), loadConfig, loadedPages) { }

        data.loadInitial()
        data.loadAfter()
        data.loadBefore()

        assertEquals(40, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1446], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1485], data.candleSeries.last())

        data.loadState.test {

            expectMostRecentItem()

            data.reload()

            assertEquals(LoadState.Loading, awaitItem())
            assertEquals(LoadState.Loaded, awaitItem())
            ensureAllEventsConsumed()

            assertEquals(40, data.candleSeries.size)
            assertEquals(CandleUtils.m5Series[1446], data.candleSeries.first())
            assertEquals(CandleUtils.m5Series[1485], data.candleSeries.last())
        }
    }

    @Test
    fun `Reload after changing interval`() = runTest {

        val params = StockChartParams("ABC", Timeframe.M5)
        val loadConfig = LoadConfig(
            initialLoadBefore = { CandleUtils.m5Series[1475].openInstant + 1.minutes },
            loadMoreCount = 10,
            initialLoadCount = 20,
            loadMoreThreshold = 5,
        )

        val loadedPages = LoadedPages()
        val data = StockChartData(FakeCandleSource(params), loadConfig, loadedPages) { }

        data.loadInitial()
        data.loadAfter()
        data.loadBefore()

        assertEquals(40, data.candleSeries.size)
        assertEquals(CandleUtils.m5Series[1446], data.candleSeries.first())
        assertEquals(CandleUtils.m5Series[1485], data.candleSeries.last())

        data.loadState.test {

            expectMostRecentItem()

            loadedPages.dropBefore()
            data.reload()

            assertEquals(LoadState.Loading, awaitItem())
            assertEquals(LoadState.Loaded, awaitItem())
            ensureAllEventsConsumed()

            assertEquals(30, data.candleSeries.size)
            assertEquals(CandleUtils.m5Series[1456], data.candleSeries.first())
            assertEquals(CandleUtils.m5Series[1485], data.candleSeries.last())
        }
    }

    private class FakeCandleSource(
        override val params: StockChartParams,
        private val emitLive: Boolean = false,
        private val generateCandleListFlow: (List<Candle>) -> Flow<List<Candle>> = { flowOf(it) },
    ) : CandleSource {

        var isInitialized = false
        var isDestroyed = false

        override suspend fun init() {
            isInitialized = true
        }

        override fun destroy() {
            isDestroyed = true
        }

        override suspend fun onLoad(interval: ClosedRange<Instant>): CandleSource.Result {

            val currentBeforeIndex = CandleUtils.m5Series
                .binarySearchByAsResult(interval.start) { it.openInstant }
                .indexOrNaturalIndex

            val currentAfterIndex = CandleUtils.m5Series
                .binarySearchByAsResult(interval.endInclusive) { it.openInstant }
                .indexOr { it - 1 }

            val startIndex = currentBeforeIndex
            val endIndexExclusive = currentAfterIndex + 1

            return CandleSource.Result(
                candles = generateCandleListFlow(
                    CandleUtils.m5Series.subList(startIndex, endIndexExclusive),
                ),
                live = when {
                    !emitLive -> null
                    else -> flow {
                        CandleUtils.m5Series
                            .subList(endIndexExclusive, endIndexExclusive + 10)
                            .forEachIndexed { index, candle ->
                                delay(10)
                                emit(IndexedValue((endIndexExclusive - startIndex) + index, candle))
                            }
                    }
                },
            )
        }

        override suspend fun getCount(interval: ClosedRange<Instant>): Int {

            val currentBeforeIndex = CandleUtils.m5Series
                .binarySearchByAsResult(interval.start) { it.openInstant }
                .indexOrNaturalIndex

            val currentAfterIndex = CandleUtils.m5Series
                .binarySearchByAsResult(interval.endInclusive) { it.openInstant }
                .indexOr { it - 1 }

            return CandleUtils.m5Series.subList(currentBeforeIndex, currentAfterIndex + 1).size
        }

        override suspend fun getBeforeInstant(
            currentBefore: Instant,
            loadCount: Int,
        ): Instant? {

            val currentBeforeIndex = CandleUtils.m5Series
                .binarySearchByAsResult(currentBefore) { it.openInstant }
                .indexOrNaturalIndex

            return CandleUtils.m5Series[currentBeforeIndex - loadCount].openInstant
        }

        override suspend fun getAfterInstant(
            currentAfter: Instant,
            loadCount: Int,
        ): Instant? {

            val currentAfterIndex = CandleUtils.m5Series
                .binarySearchByAsResult(currentAfter) { it.openInstant }
                .indexOr { it - 1 }

            return CandleUtils.m5Series[currentAfterIndex + loadCount].openInstant
        }
    }
}
