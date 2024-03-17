package com.saurabhsandav.core.trading.backtest

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import java.math.BigDecimal

class BacktestAccount(
    initialBalance: BigDecimal,
) {

    var balance: BigDecimal = initialBalance
        private set

    private val _transactions = MutableStateFlow(persistentListOf<BacktestTransaction>())
    val transactions = _transactions.asStateFlow()

    fun addTransaction(
        instant: Instant,
        value: BigDecimal,
    ) {

        // Update Transaction
        _transactions.update { list -> list.add(BacktestTransaction(instant, value)) }

        // Update balance
        balance += value
    }
}

data class BacktestTransaction(
    val instant: Instant,
    val value: BigDecimal,
)
