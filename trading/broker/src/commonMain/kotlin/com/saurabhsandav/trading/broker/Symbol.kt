package com.saurabhsandav.trading.broker

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import kotlin.time.Instant

data class Symbol(
    val id: SymbolId,
    val brokerId: BrokerId,
    val exchange: String,
    val exchangeToken: String,
    val instrument: Instrument,
    val ticker: String,
    val tickSize: KBigDecimal,
    val lotSize: KBigDecimal,
    val description: String? = null,
    val expiry: Instant? = null,
    val strikePrice: KBigDecimal? = null,
    val optionType: OptionType? = null,
)

enum class OptionType {
    Call,
    Put,
    ;

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<OptionType, String> {
        override fun decode(databaseValue: String): OptionType = OptionType.valueOf(databaseValue)

        override fun encode(value: OptionType): String = value.name
    }
}
