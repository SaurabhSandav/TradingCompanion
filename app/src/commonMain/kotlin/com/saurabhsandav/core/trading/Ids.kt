package com.saurabhsandav.core.trading

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ProfileId(
    val value: Long,
) {

    override fun toString(): String = value.toString()
}

object ProfileIdColumnAdapter : ColumnAdapter<ProfileId, Long> {
    override fun decode(databaseValue: Long): ProfileId = ProfileId(databaseValue)

    override fun encode(value: ProfileId): Long = value.value
}
