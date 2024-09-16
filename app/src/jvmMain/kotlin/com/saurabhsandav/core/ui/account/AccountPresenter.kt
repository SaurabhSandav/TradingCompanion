package com.saurabhsandav.core.ui.account

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.mapList
import kotlinx.coroutines.CoroutineScope

internal class AccountPresenter(
    appDispatchers: AppDispatchers,
    coroutineScope: CoroutineScope,
    appDB: AppDB,
) {

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        val transactions by remember {
            appDB.accountTransactionQueries
                .getAll()
                .asFlow()
                .mapToList(appDispatchers.IO)
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
