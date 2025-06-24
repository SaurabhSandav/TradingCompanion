package com.saurabhsandav.core.trading.record

import androidx.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.saurabhsandav.core.thirdparty.sqldelight.paging.QueryPagingSource
import com.saurabhsandav.core.trading.record.model.ReviewId
import com.saurabhsandav.core.trading.record.model.TradeId
import com.saurabhsandav.core.utils.withoutNanoseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock

class Reviews internal constructor(
    private val coroutineContext: CoroutineContext,
    private val tradesDB: TradesDB,
) {

    suspend fun new(
        title: String,
        tradeIds: List<TradeId>,
        review: String,
    ): ReviewId = withContext(coroutineContext) {

        return@withContext tradesDB.transactionWithResult {

            tradesDB.reviewQueries.insert(
                title = title,
                tradeIds = tradeIds,
                review = review,
                created = Clock.System.now().withoutNanoseconds(),
            )

            tradesDB.tradesDBUtilsQueries.lastInsertedRowId().executeAsOne().let(::ReviewId)
        }
    }

    suspend fun setTitle(
        id: ReviewId,
        title: String,
    ) = withContext(coroutineContext) {

        tradesDB.reviewQueries.setTitle(
            id = id,
            title = title,
        )
    }

    suspend fun update(
        id: ReviewId,
        review: String,
        tradeIds: List<TradeId>,
    ) = withContext(coroutineContext) {

        tradesDB.reviewQueries.update(
            id = id,
            review = review,
            tradeIds = tradeIds,
        )
    }

    suspend fun togglePinned(id: ReviewId) = withContext(coroutineContext) {
        tradesDB.reviewQueries.toggleIsPinned(id)
    }

    suspend fun delete(id: ReviewId) = withContext(coroutineContext) {
        tradesDB.reviewQueries.delete(id)
    }

    fun exists(id: Long): Flow<Boolean> {
        return tradesDB.reviewQueries.exists(ReviewId(id)).asFlow().mapToOne(coroutineContext)
    }

    fun getById(id: ReviewId): Flow<Review> {
        return tradesDB.reviewQueries.getById(id).asFlow().mapToOne(coroutineContext)
    }

    fun getAllPagingSource(): PagingSource<Int, Review> = QueryPagingSource(
        countQuery = tradesDB.reviewQueries.getAllCount(),
        transacter = tradesDB.reviewQueries,
        context = coroutineContext,
        queryProvider = tradesDB.reviewQueries::getAllPaged,
    )

    fun getPinnedCount(): Flow<Long> {
        return tradesDB.reviewQueries.getPinnedCount().asFlow().mapToOne(coroutineContext)
    }

    fun getUnpinnedCount(): Flow<Long> {
        return tradesDB.reviewQueries.getUnpinnedCount().asFlow().mapToOne(coroutineContext)
    }
}
