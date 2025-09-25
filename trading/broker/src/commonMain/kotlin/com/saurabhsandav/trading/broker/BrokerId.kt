package com.saurabhsandav.trading.broker

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class BrokerId(
    val value: String,
)

object BrokerIdColumnAdapter : ColumnAdapter<BrokerId, String> {

    override fun decode(databaseValue: String): BrokerId = BrokerId(databaseValue)

    override fun encode(value: BrokerId): String = value.value
}
