package ui.account

import androidx.compose.runtime.Immutable

@Immutable
internal data class AccountState(
    val transactions: List<Transaction> = emptyList(),
)

@Immutable
internal data class Transaction(
    val date: String,
    val amount: String,
    val type: String,
    val note: String,
)
