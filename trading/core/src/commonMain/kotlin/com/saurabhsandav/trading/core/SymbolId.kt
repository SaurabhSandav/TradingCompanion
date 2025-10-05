package com.saurabhsandav.trading.core

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class SymbolId(
    val value: String,
)
