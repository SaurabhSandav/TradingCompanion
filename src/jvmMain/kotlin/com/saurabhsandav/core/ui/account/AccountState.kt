package com.saurabhsandav.core.ui.account

internal data class AccountState(
    val transactions: List<Transaction>,
)

internal data class Transaction(
    val date: String,
    val amount: String,
    val type: String,
    val note: String,
)
