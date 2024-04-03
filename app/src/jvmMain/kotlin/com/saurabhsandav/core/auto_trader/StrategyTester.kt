package com.saurabhsandav.core.auto_trader

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trades.stats.buildStats
import com.saurabhsandav.core.trading.autotrader.ReplayAutoTrader
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

suspend fun main() {

    val appModule = AppModule()

    val replayTrader = ReplayAutoTrader(
        candleRepo = appModule.candleRepo,
        tradingProfiles = appModule.tradingProfiles,
    )

    val tz = TimeZone.currentSystemDefault()

    val tickers = listOf("NTPC")
    val from = LocalDateTime(2023, 1, 1, 0, 0)
    val to = LocalDateTime(2024, 1, 1, 0, 0)

    val record = replayTrader.trade(
        tickers = tickers,
        from = from.toInstant(tz),
        to = to.toInstant(tz),
        strategy = emaCrossoverStrategy(),
    )

    record.buildStats().first().also { println(it?.prettyPrint()) }
}

private fun Any.prettyPrint(): String {

    var indentLevel = 0
    val indentWidth = 4

    fun padding() = "".padStart(indentLevel * indentWidth)

    val toString = toString()

    val stringBuilder = StringBuilder(toString.length)

    var i = 0
    while (i < toString.length) {
        when (val char = toString[i]) {
            '(', '[', '{' -> {
                indentLevel++
                stringBuilder.appendLine(char).append(padding())
            }

            ')', ']', '}' -> {
                indentLevel--
                stringBuilder.appendLine().append(padding()).append(char)
            }

            ',' -> {
                stringBuilder.appendLine(char).append(padding())
                // ignore space after comma as we have added a newline
                val nextChar = toString.getOrElse(i + 1) { char }
                if (nextChar == ' ') i++
            }

            '=' -> {
                stringBuilder.append(' ').append(char).append(' ')
            }

            else -> {
                stringBuilder.append(char)
            }
        }
        i++
    }

    return stringBuilder.toString()
}
