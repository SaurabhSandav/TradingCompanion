package com.saurabhsandav.trading.record.model

import app.cash.sqldelight.ColumnAdapter

object TradeIdListColumnAdapter : ColumnAdapter<List<TradeId>, String> {

    override fun decode(databaseValue: String): List<TradeId> {
        return databaseValue.removeSurrounding(prefix = "[", suffix = "]")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { TradeId(it.toLong()) }
    }

    override fun encode(value: List<TradeId>): String = value.map { it.value }.joinToString()
}
