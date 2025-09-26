package com.saurabhsandav.core

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.utils.InstantColumnAdapter
import com.saurabhsandav.core.utils.KBigDecimalColumnAdapter
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.record.InstrumentColumnAdapter
import com.saurabhsandav.trading.record.SymbolIdColumnAdapter

fun AppDB(driver: SqlDriver) = AppDB(
    driver = driver,
    TradingProfileAdapter = TradingProfile.Adapter(
        idAdapter = ProfileId.ColumnAdapter,
        tradeCountAdapter = IntColumnAdapter,
        tradeCountOpenAdapter = IntColumnAdapter,
    ),
    CachedSymbolAdapter = CachedSymbol.Adapter(
        idAdapter = SymbolIdColumnAdapter,
        brokerIdAdapter = BrokerId.ColumnAdapter,
        instrumentAdapter = InstrumentColumnAdapter,
        tickSizeAdapter = KBigDecimalColumnAdapter,
        quantityMultiplierAdapter = KBigDecimalColumnAdapter,
    ),
    SymbolDownloadTimestampAdapter = SymbolDownloadTimestamp.Adapter(
        brokerIdAdapter = BrokerId.ColumnAdapter,
        timestampAdapter = InstantColumnAdapter,
    ),
)
