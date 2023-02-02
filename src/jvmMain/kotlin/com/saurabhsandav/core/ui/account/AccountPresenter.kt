package com.saurabhsandav.core.ui.account

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.mapList
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

internal class AccountPresenter(
    coroutineScope: CoroutineScope,
    private val appModule: AppModule,
) {

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        val transactions by remember {
            appModule.appDB.accountTransactionQueries
                .getAll()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .mapList { acTransaction ->
                    Transaction(
                        date = acTransaction.date,
                        amount = acTransaction.amount,
                        type = acTransaction.type,
                        note = acTransaction.note,
                    )
                }
        }.collectAsState(emptyList())

        return@launchMolecule AccountState(transactions)
    }
}
