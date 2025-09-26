package com.saurabhsandav.trading.broker

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class BrokerId(
    val value: String,
) {

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<BrokerId, String> {

        override fun decode(databaseValue: String): BrokerId = BrokerId(databaseValue)

        override fun encode(value: BrokerId): String = value.value
    }
}
