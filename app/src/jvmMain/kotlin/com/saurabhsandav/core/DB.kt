package com.saurabhsandav.core

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.saurabhsandav.core.trades.model.ProfileIdColumnAdapter
import com.saurabhsandav.core.trading.data.CandleDB
import com.saurabhsandav.core.trading.data.CheckedRange
import com.saurabhsandav.core.utils.InstantColumnAdapter

fun AppDB(driver: SqlDriver) = AppDB(
    driver = driver,
    TradingProfileAdapter = TradingProfile.Adapter(
        idAdapter = ProfileIdColumnAdapter,
        tradeCountAdapter = IntColumnAdapter,
        tradeCountOpenAdapter = IntColumnAdapter,
    ),
)

fun CandleDB(driver: SqlDriver) = CandleDB(
    driver = driver,
    CheckedRangeAdapter = CheckedRange.Adapter(
        fromEpochSecondsAdapter = InstantColumnAdapter,
        toEpochSecondsAdapter = InstantColumnAdapter,
    ),
)
