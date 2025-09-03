package com.saurabhsandav.trading.backtest

import com.saurabhsandav.kbigdecimal.KBigDecimal
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

class BacktestAccount(
    initialBalance: KBigDecimal,
) {

    var balance: KBigDecimal = initialBalance
        private set

    private val _transactions = MutableStateFlow(persistentListOf<BacktestTransaction>())
    val transactions = _transactions.asStateFlow()

    fun addTransaction(
        instant: Instant,
        value: KBigDecimal,
    ) {

        // Update Transaction
        _transactions.update { list -> list.add(BacktestTransaction(instant, value)) }

        // Update balance
        balance += value
    }
}

data class BacktestTransaction(
    val instant: Instant,
    val value: KBigDecimal,
)
