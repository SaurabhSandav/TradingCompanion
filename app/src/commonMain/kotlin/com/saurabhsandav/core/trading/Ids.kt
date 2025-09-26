package com.saurabhsandav.core.trading

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ProfileId(
    val value: Long,
) {

    override fun toString(): String = value.toString()

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<ProfileId, Long> {
        override fun decode(databaseValue: Long): ProfileId = ProfileId(databaseValue)

        override fun encode(value: ProfileId): Long = value.value
    }
}
