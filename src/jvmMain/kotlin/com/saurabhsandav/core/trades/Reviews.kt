package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.saurabhsandav.core.trades.model.ReviewId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.utils.withoutNanoseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class Reviews(
    private val tradesDB: TradesDB,
) {

    suspend fun new(
        title: String,
        tradeIds: List<TradeId>,
        review: String,
        isMarkdown: Boolean,
    ): ReviewId = withContext(Dispatchers.IO) {

        return@withContext tradesDB.transactionWithResult {

            tradesDB.reviewQueries.insert(
                title = title,
                tradeIds = tradeIds,
                review = review,
                created = Clock.System.now().withoutNanoseconds(),
                isMarkdown = isMarkdown,
            )

            tradesDB.tradesDBUtilsQueries.lastInsertedRowId().executeAsOne().let(::ReviewId)
        }
    }

    suspend fun setTitle(
        id: ReviewId,
        title: String,
    ) = withContext(Dispatchers.IO) {

        tradesDB.reviewQueries.setTitle(
            id = id,
            title = title,
        )
    }

    suspend fun toggleIsMarkdown(id: ReviewId) = withContext(Dispatchers.IO) {

        tradesDB.reviewQueries.toggleMarkdown(id = id)
    }

    suspend fun update(
        id: ReviewId,
        review: String,
        tradeIds: List<TradeId>,
    ) = withContext(Dispatchers.IO) {

        tradesDB.reviewQueries.update(
            id = id,
            review = review,
            tradeIds = tradeIds,
        )
    }

    suspend fun togglePinned(id: ReviewId) = withContext(Dispatchers.IO) {
        tradesDB.reviewQueries.toggleIsPinned(id)
    }

    suspend fun delete(id: ReviewId) = withContext(Dispatchers.IO) {
        tradesDB.reviewQueries.delete(id)
    }

    fun exists(id: Long): Flow<Boolean> {
        return tradesDB.reviewQueries.exists(ReviewId(id)).asFlow().mapToOne(Dispatchers.IO)
    }

    fun getPinned(): Flow<List<Review>> {
        return tradesDB.reviewQueries.getPinned().asFlow().mapToList(Dispatchers.IO)
    }

    fun getUnPinned(): Flow<List<Review>> {
        return tradesDB.reviewQueries.getUnPinned().asFlow().mapToList(Dispatchers.IO)
    }

    fun getById(id: ReviewId): Flow<Review> {
        return tradesDB.reviewQueries.getById(id).asFlow().mapToOne(Dispatchers.IO)
    }
}
