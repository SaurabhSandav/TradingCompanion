package com.saurabhsandav.core

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.saurabhsandav.core.trading.ProfileIdColumnAdapter
import com.saurabhsandav.core.utils.InstantColumnAdapter
import com.saurabhsandav.trading.candledata.CandleDB
import com.saurabhsandav.trading.candledata.CheckedRange

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
