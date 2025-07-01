package com.saurabhsandav.trading.record

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.model.TradeNoteId
import com.saurabhsandav.trading.record.utils.withoutNanoseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock

class Notes internal constructor(
    private val coroutineContext: CoroutineContext,
    private val tradesDB: TradesDB,
) {

    fun getForTrade(id: TradeId): Flow<List<TradeNote>> {
        return tradesDB.tradeNoteQueries.getByTrade(id).asFlow().mapToList(coroutineContext)
    }

    suspend fun add(
        tradeId: TradeId,
        note: String,
    ) = withContext(coroutineContext) {

        val now = Clock.System.now().withoutNanoseconds()

        tradesDB.tradeNoteQueries.insert(
            tradeId = tradeId,
            note = note,
            added = now,
            lastEdited = null,
        )
    }

    suspend fun update(
        id: TradeNoteId,
        note: String,
    ) = withContext(coroutineContext) {

        tradesDB.tradeNoteQueries.update(
            id = id,
            note = note,
            lastEdited = Clock.System.now().withoutNanoseconds(),
        )
    }

    suspend fun delete(id: TradeNoteId) = withContext(coroutineContext) {
        tradesDB.tradeNoteQueries.delete(id)
    }
}
