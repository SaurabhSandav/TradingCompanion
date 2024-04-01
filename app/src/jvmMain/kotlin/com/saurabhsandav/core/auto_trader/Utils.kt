package com.saurabhsandav.core.auto_trader

import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Indicator
import com.saurabhsandav.core.trading.isLong
import java.math.BigDecimal
import java.math.RoundingMode

fun List<Candle>.higherHighsAndLows(): Boolean {

    for (i in indices) {

        if (i == lastIndex) break

        val higherHighs = get(i).high < get(i + 1).high
        val higherLows = get(i).low < get(i + 1).low

        if (!(higherHighs && higherLows)) return false
    }

    return true
}

fun List<Candle>.pctChange(): BigDecimal {

    val start = first().open
    val end = last().close

    return ((end - start).divide(start, 10, RoundingMode.HALF_EVEN)) * 100.toBigDecimal()
}

fun List<Candle>.lowerHighsAndLows(): Boolean {

    for (i in indices) {

        if (i == lastIndex) break

        val lowerHighs = get(i).high > get(i + 1).high
        val lowerLows = get(i).low > get(i + 1).low

        if (!(lowerHighs && lowerLows)) return false
    }

    return true
}

fun List<Candle>.risingVolume(): Boolean {

    for (i in indices) {

        if (i == lastIndex) break

        val risingVolume = get(i).volume < get(i + 1).volume

        if (!risingVolume) return false
    }

    return true
}

fun List<Candle>.bullishAll(): Boolean = all { it.isLong }

fun List<Candle>.bearishAll(): Boolean = all { !it.isLong }

private val CandleSeries.lastFinishedIndex: Int
    get() = lastIndex - 1

private operator fun CandleSeries.get(
    from: Int,
    toInclusive: Int,
): List<Candle> = subList(from, toInclusive + 1)

fun <T : Any> Indicator<T>.takeLast(n: Int): List<T> {
    val lastIndex = candleSeries.lastIndex
    return get(lastIndex - n - 1, lastIndex)
}

fun BigDecimal.round(): BigDecimal = round("0.05".toBigDecimal(), RoundingMode.HALF_EVEN)

fun BigDecimal.round(
    increment: BigDecimal,
    roundingMode: RoundingMode,
): BigDecimal {

    return when {
        // 0 increment does not make much sense, but prevent division by 0
        increment.signum() == 0 -> this
        else -> {
            val divided = divide(increment, 0, roundingMode)
            val result = divided.multiply(increment)
            result
        }
    }
}

val Candle.travel: BigDecimal
    get() = if (isLong) close - open else open - close
