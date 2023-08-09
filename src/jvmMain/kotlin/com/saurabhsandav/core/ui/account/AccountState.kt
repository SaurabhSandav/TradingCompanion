package com.saurabhsandav.core.ui.account

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class AccountState(
    val transactions: ImmutableList<Transaction>,
)

@Immutable
internal data class Transaction(
    val date: String,
    val amount: String,
    val type: String,
    val note: String,
)
