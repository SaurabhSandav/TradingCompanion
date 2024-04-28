package com.saurabhsandav.core.paging_sqldelight

import app.cash.paging.*
import app.cash.sqldelight.*
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class OffsetQueryPagingSource<RowType : Any>(
    private val queryProvider: (limit: Int, offset: Int) -> Query<RowType>,
    private val countQuery: Query<Int>,
    private val transacter: TransacterBase,
    private val context: CoroutineContext,
    private val initialOffset: Int,
) : QueryPagingSource<Int, RowType>() {

    override val jumpingSupported get() = true

    override suspend fun load(
        params: PagingSourceLoadParams<Int>,
    ): PagingSourceLoadResult<Int, RowType> = withContext(context) {
        val key = params.key ?: initialOffset
        val limit = when (params) {
            is PagingSourceLoadParamsPrepend<*> -> minOf(key, params.loadSize)
            else -> params.loadSize
        }
        val getPagingSourceLoadResult: TransactionCallbacks.() -> PagingSourceLoadResultPage<Int, RowType> = {
            val count = countQuery.executeAsOne()
            val offset = when (params) {
                is PagingSourceLoadParamsPrepend<*> -> maxOf(0, key - params.loadSize)
                is PagingSourceLoadParamsAppend<*> -> key
                is PagingSourceLoadParamsRefresh<*> -> if (key >= count) maxOf(0, count - params.loadSize) else key
                else -> error("Unknown PagingSourceLoadParams ${params::class}")
            }
            val data = queryProvider(limit, offset)
                .also { currentQuery = it }
                .executeAsList()
            val nextPosToLoad = offset + data.size
            PagingSourceLoadResultPage(
                data = data,
                prevKey = offset.takeIf { it > 0 && data.isNotEmpty() },
                nextKey = nextPosToLoad.takeIf { data.isNotEmpty() && data.size >= limit && it < count },
                itemsBefore = offset,
                itemsAfter = maxOf(0, count - nextPosToLoad),
            )
        }
        val loadResult = when (transacter) {
            is Transacter -> transacter.transactionWithResult(bodyWithReturn = getPagingSourceLoadResult)
            is SuspendingTransacter -> transacter.transactionWithResult(bodyWithReturn = getPagingSourceLoadResult)
        }
        if (invalid) PagingSourceLoadResultInvalid() else loadResult
    }

    override fun getRefreshKey(state: PagingState<Int, RowType>) =
        state.anchorPosition?.let { maxOf(0, it - (state.config.initialLoadSize / 2)) }
}
