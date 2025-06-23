package com.saurabhsandav.trading.core

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

enum class Timeframe(
    val seconds: Long,
) {
    M1(1.minutes.inWholeSeconds),
    M3(3.minutes.inWholeSeconds),
    M5(5.minutes.inWholeSeconds),
    M15(15.minutes.inWholeSeconds),
    M30(30.minutes.inWholeSeconds),
    H1(60.minutes.inWholeSeconds),
    H4(240.minutes.inWholeSeconds),
    D1(1.days.inWholeSeconds),
    ;

    companion object {

        fun fromSeconds(seconds: Long): Timeframe? = when (seconds) {
            M1.seconds -> M1
            M5.seconds -> M5
            M3.seconds -> M3
            M15.seconds -> M15
            M30.seconds -> M30
            H1.seconds -> H1
            H4.seconds -> H4
            D1.seconds -> D1
            else -> null
        }
    }
}
