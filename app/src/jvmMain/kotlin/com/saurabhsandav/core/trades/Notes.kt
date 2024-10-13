package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.trades.model.TradeNoteId
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.withoutNanoseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class Notes internal constructor(
    private val appDispatchers: AppDispatchers,
    private val tradesDB: TradesDB,
) {

    fun getForTrade(id: TradeId): Flow<List<TradeNote>> {
        return tradesDB.tradeNoteQueries.getByTrade(id).asFlow().mapToList(appDispatchers.IO)
    }

    suspend fun add(
        tradeId: TradeId,
        note: String,
        isMarkdown: Boolean,
    ) = withContext(appDispatchers.IO) {

        val now = Clock.System.now().withoutNanoseconds()

        tradesDB.tradeNoteQueries.insert(
            tradeId = tradeId,
            note = note,
            added = now,
            lastEdited = null,
            isMarkdown = isMarkdown,
        )
    }

    suspend fun update(
        id: TradeNoteId,
        note: String,
        isMarkdown: Boolean,
    ) = withContext(appDispatchers.IO) {

        tradesDB.tradeNoteQueries.update(
            id = id,
            note = note,
            lastEdited = Clock.System.now().withoutNanoseconds(),
            isMarkdown = isMarkdown,
        )
    }

    suspend fun delete(id: TradeNoteId) = withContext(appDispatchers.IO) {
        tradesDB.tradeNoteQueries.delete(id)
    }
}
