package com.saurabhsandav.core

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.saurabhsandav.core.trading.ProfileIdColumnAdapter
import com.saurabhsandav.core.utils.InstantColumnAdapter
import com.saurabhsandav.core.utils.KBigDecimalColumnAdapter
import com.saurabhsandav.trading.broker.BrokerIdColumnAdapter
import com.saurabhsandav.trading.broker.OptionType
import com.saurabhsandav.trading.record.InstrumentColumnAdapter
import com.saurabhsandav.trading.record.SymbolIdColumnAdapter

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
        instrumentAdapter = InstrumentColumnAdapter,
        tickSizeAdapter = KBigDecimalColumnAdapter,
        lotSizeAdapter = KBigDecimalColumnAdapter,
        expiryAdapter = InstantColumnAdapter,
        strikePriceAdapter = KBigDecimalColumnAdapter,
        optionTypeAdapter = OptionType.ColumnAdapter,
    ),
    SymbolDownloadTimestampAdapter = SymbolDownloadTimestamp.Adapter(
        brokerIdAdapter = BrokerIdColumnAdapter,
        timestampAdapter = InstantColumnAdapter,
    ),
)
