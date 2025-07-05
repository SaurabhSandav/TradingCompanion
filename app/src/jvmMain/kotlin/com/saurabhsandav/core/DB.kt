package com.saurabhsandav.core

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.saurabhsandav.core.trading.ProfileIdColumnAdapter
import com.saurabhsandav.core.utils.BigDecimalColumnAdapter
import com.saurabhsandav.core.utils.InstantColumnAdapter
import com.saurabhsandav.core.utils.InstantLongColumnAdapter
import com.saurabhsandav.trading.broker.BrokerIdColumnAdapter
import com.saurabhsandav.trading.candledata.CandleDB
import com.saurabhsandav.trading.candledata.CheckedRange
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolIdColumnAdapter

fun AppDB(driver: SqlDriver) = AppDB(
    driver = driver,
    TradingProfileAdapter = TradingProfile.Adapter(
        idAdapter = ProfileIdColumnAdapter,
        tradeCountAdapter = IntColumnAdapter,
        tradeCountOpenAdapter = IntColumnAdapter,
    ),
    CachedSymbolAdapter = CachedSymbol.Adapter(
        idAdapter = SymbolIdColumnAdapter,
        brokerIdAdapter = BrokerIdColumnAdapter,
        instrumentAdapter = Instrument.ColumnAdapter,
        tickSizeAdapter = BigDecimalColumnAdapter,
        quantityMultiplierAdapter = BigDecimalColumnAdapter,
    ),
    SymbolDownloadTimestampAdapter = SymbolDownloadTimestamp.Adapter(
        brokerIdAdapter = BrokerIdColumnAdapter,
        timestampAdapter = InstantColumnAdapter,
    ),
)

fun CandleDB(driver: SqlDriver) = CandleDB(
    driver = driver,
    CheckedRangeAdapter = CheckedRange.Adapter(
        fromEpochSecondsAdapter = InstantLongColumnAdapter,
        toEpochSecondsAdapter = InstantLongColumnAdapter,
    ),
)
