package com.saurabhsandav.core.trades.model

actual enum class TransactionType(val strValue: String) {
    Credit("credit"),
    Debit("debit");

    companion object {

        fun fromString(strValue: String) = when (strValue.lowercase()) {
            "credit" -> Credit
            "debit" -> Debit
            else -> error("Invalid transaction type")
        }
    }

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<TransactionType, String> {

        override fun decode(databaseValue: String): TransactionType = TransactionType.fromString(databaseValue)

        override fun encode(value: TransactionType): String = value.strValue
    }
}
