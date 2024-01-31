package com.saurabhsandav.core

import com.github.michaelbull.result.get
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.CandleRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.*
import org.roboquant.Roboquant
import org.roboquant.common.Asset
import org.roboquant.feeds.Event
import org.roboquant.feeds.EventChannel
import org.roboquant.feeds.Feed
import org.roboquant.feeds.PriceBar
import org.roboquant.loggers.MemoryLogger
import org.roboquant.metrics.AccountMetric
import org.roboquant.strategies.EMAStrategy
import org.roboquant.common.Timeframe as RQTimeframe

fun main() {

    val strategy = EMAStrategy() // (1)
    val metric = AccountMetric() // (2)
    val logger = MemoryLogger()
    val roboquant = Roboquant(
        strategy = strategy,
        metrics = listOf(metric),
        logger = logger,
    ) // (3)

    val tz = TimeZone.currentSystemDefault()
    val intervalStart = LocalDateTime(year = 2023, monthNumber = 1, dayOfMonth = 1, hour = 0, minute = 0).toInstant(tz)
    val intervalEnd = LocalDateTime(year = 2024, monthNumber = 1, dayOfMonth = 1, hour = 0, minute = 0).toInstant(tz)

    val appModule = AppModule()

    val feed = FyersNSEFeed(
        ticker = "NTPC",
        candleTimeframe = Timeframe.M5,
        interval = intervalStart..intervalEnd,
        candleRepo = appModule.candleRepo,
    )
    roboquant.run(feed) // (5)

    val run = logger.getRuns().first()

    logger.getMetricNames().forEach { metricName ->
        println(
            "$metricName: ${logger.getMetric(metricName, run).first()}, ${
                logger.getMetric(metricName, run).last()
            }"
        )
    }
}

internal class FyersNSEFeed(
    private val ticker: String,
    private val candleTimeframe: Timeframe,
    private val interval: ClosedRange<Instant>,
    private val candleRepo: CandleRepository,
) : Feed {

    override val timeframe: RQTimeframe = RQTimeframe(
        start = interval.start.toJavaInstant(),
        end = interval.endInclusive.toJavaInstant(),
        inclusive = true,
    )

    override suspend fun play(channel: EventChannel) {

        val candles = candleRepo.getCandles(
            ticker = ticker,
            timeframe = candleTimeframe,
            from = interval.start,
            to = interval.endInclusive,
            includeFromCandle = false,
        ).get()!!.first()

        candles.forEach { candle ->

            val priceBar = PriceBar(
                asset = Asset(symbol = ticker),
                open = candle.open,
                high = candle.high,
                low = candle.low,
                close = candle.close,
                volume = candle.volume,
            )
            val actions = listOf(priceBar)
            val event = Event(actions, candle.openInstant.toJavaInstant())

            channel.send(event)
        }
    }
}
