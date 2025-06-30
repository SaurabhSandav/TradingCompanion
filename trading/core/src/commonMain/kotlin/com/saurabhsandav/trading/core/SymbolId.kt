package com.saurabhsandav.trading.core

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class SymbolId(
    val value: String,
)

object SymbolIdColumnAdapter : ColumnAdapter<SymbolId, String> {

    override fun decode(databaseValue: String): SymbolId = SymbolId(databaseValue)

    override fun encode(value: SymbolId): String = value.value
}
