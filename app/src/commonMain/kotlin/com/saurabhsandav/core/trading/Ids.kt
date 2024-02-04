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

@JvmInline
value class AutoTraderScriptId(
    val value: Long,
) {

    override fun toString(): String = value.toString()
}

object ProfileIdColumnAdapter : ColumnAdapter<ProfileId, Long> {
    override fun decode(databaseValue: Long): ProfileId = ProfileId(databaseValue)

    override fun encode(value: ProfileId): Long = value.value
}

object AutoTraderScriptIdColumnAdapter : ColumnAdapter<AutoTraderScriptId, Long> {

    override fun decode(databaseValue: Long): AutoTraderScriptId = AutoTraderScriptId(databaseValue)

    override fun encode(value: AutoTraderScriptId): Long = value.value
}
