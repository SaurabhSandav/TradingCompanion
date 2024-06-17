package com.saurabhsandav.core.trades

import androidx.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.saurabhsandav.core.paging_sqldelight.QueryPagingSource
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

    fun getById(id: ReviewId): Flow<Review> {
        return tradesDB.reviewQueries.getById(id).asFlow().mapToOne(Dispatchers.IO)
    }

    fun getAllPagingSource(): PagingSource<Int, Review> = QueryPagingSource(
        countQuery = tradesDB.reviewQueries.getAllCount(),
        transacter = tradesDB.reviewQueries,
        context = Dispatchers.IO,
        queryProvider = tradesDB.reviewQueries::getAllPaged,
    )

    fun getPinnedCount(): Flow<Long> {
        return tradesDB.reviewQueries.getPinnedCount().asFlow().mapToOne(Dispatchers.IO)
    }

    fun getUnpinnedCount(): Flow<Long> {
        return tradesDB.reviewQueries.getUnpinnedCount().asFlow().mapToOne(Dispatchers.IO)
    }
}
