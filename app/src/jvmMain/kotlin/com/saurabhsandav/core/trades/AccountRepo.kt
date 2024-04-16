package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.trades.model.TransactionType.Credit
import com.saurabhsandav.core.trades.model.TransactionType.Debit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import java.math.BigDecimal

class AccountRepo(
    private val tradesDB: TradesDB,
) {

    private var balance = tradesDB.accountTransactionQueries
        .getLastBalance()
        .asFlow()
        .mapToOneOrNull(Dispatchers.IO)
        .map { it ?: BigDecimal.ZERO }

    private val transactions: Flow<List<AccountTransaction>>
        get() = tradesDB.accountTransactionQueries.getAll().asFlow().mapToList(Dispatchers.IO)

    suspend fun addTransaction(
        instant: Instant,
        transaction: BigDecimal,
    ) = withContext(Dispatchers.IO) {

        tradesDB.accountTransactionQueries.transaction {

            val lastBalance = tradesDB.accountTransactionQueries
                .getLastBalance()
                .executeAsOneOrNull()
                ?: BigDecimal.ZERO

            val newBalance = lastBalance + transaction

            tradesDB.accountTransactionQueries.insert(
                timestamp = instant,
                amount = transaction.abs(),
                type = if (transaction >= BigDecimal.ZERO) Credit else Debit,
                balance = newBalance,
                note = "",
            )
        }
    }
}
