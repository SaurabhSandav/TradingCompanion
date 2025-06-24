package com.saurabhsandav.core.trading.record

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.saurabhsandav.core.trading.record.model.TradeId
import com.saurabhsandav.core.trading.record.model.TradeTagId
import com.saurabhsandav.core.utils.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class Tags internal constructor(
    private val appDispatchers: AppDispatchers,
    private val tradesDB: TradesDB,
) {

    fun getAll(): Flow<List<TradeTag>> {
        return tradesDB.tradeTagQueries.getAll().asFlow().mapToList(appDispatchers.IO)
    }

    fun getById(id: TradeTagId): Flow<TradeTag> {
        return tradesDB.tradeTagQueries.getById(id).asFlow().mapToOne(appDispatchers.IO)
    }

    fun getByIds(ids: List<TradeTagId>): Flow<List<TradeTag>> {
        return tradesDB.tradeTagQueries.getAllByIds(ids).asFlow().mapToList(appDispatchers.IO)
    }

    fun getSuggested(
        filter: String,
        ignoreIds: List<TradeTagId> = emptyList(),
    ): Flow<List<TradeTag>> {
        return tradesDB.tradeTagQueries.getSuggestedTags(ignoreIds, filter).asFlow().mapToList(appDispatchers.IO)
    }

    fun getForTrade(id: TradeId): Flow<List<TradeTag>> {
        return tradesDB.tradeToTagMapQueries.getTagsByTrade(id).asFlow().mapToList(appDispatchers.IO)
    }

    fun getSuggestedForTrades(
        tradeIds: List<TradeId>,
        filterQuery: String,
    ): Flow<List<TradeTag>> {
        return tradesDB.tradeToTagMapQueries
            .getSuggestedTagsForTrades(tradeIds, filterQuery)
            .asFlow()
            .mapToList(appDispatchers.IO)
    }

    suspend fun create(
        name: String,
        description: String,
        color: Int?,
    ) = withContext(appDispatchers.IO) {

        tradesDB.tradeTagQueries.insert(
            name = name,
            description = description,
            color = color,
        )
    }

    suspend fun update(
        id: TradeTagId,
        name: String,
        description: String,
        color: Int?,
    ) = withContext(appDispatchers.IO) {

        tradesDB.tradeTagQueries.update(
            id = id,
            name = name,
            description = description,
            color = color,
        )
    }

    suspend fun delete(id: TradeTagId) = withContext(appDispatchers.IO) {
        tradesDB.tradeTagQueries.delete(id)
    }

    suspend fun isNameUnique(
        name: String,
        ignoreTagId: TradeTagId? = null,
    ): Boolean = withContext(appDispatchers.IO) {
        return@withContext tradesDB.tradeTagQueries
            .run {
                when {
                    ignoreTagId == null -> isTagNameUnique(name)
                    else -> isTagNameUniqueIgnoreId(name, ignoreTagId)
                }
            }
            .executeAsOne()
    }

    suspend fun add(
        tradeIds: List<TradeId>,
        tagId: TradeTagId,
    ) = withContext(appDispatchers.IO) {

        tradesDB.transaction {

            tradeIds.forEach { tradeId ->

                tradesDB.tradeToTagMapQueries.insert(
                    tradeId = tradeId,
                    tagId = tagId,
                )
            }
        }
    }

    suspend fun remove(
        tradeId: TradeId,
        tagId: TradeTagId,
    ) = withContext(appDispatchers.IO) {

        tradesDB.tradeToTagMapQueries.delete(
            tradeId = tradeId,
            tagId = tagId,
        )
    }
}
