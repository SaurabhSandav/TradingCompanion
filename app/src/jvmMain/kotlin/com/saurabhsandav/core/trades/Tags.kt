package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.utils.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class Tags internal constructor(
    private val appDispatchers: AppDispatchers,
    private val tradesDB: TradesDB,
) {

    fun getAllTags(): Flow<List<TradeTag>> {
        return tradesDB.tradeTagQueries.getAll().asFlow().mapToList(appDispatchers.IO)
    }

    fun getTagById(id: TradeTagId): Flow<TradeTag> {
        return tradesDB.tradeTagQueries.getById(id).asFlow().mapToOne(appDispatchers.IO)
    }

    fun getTagsByIds(ids: List<TradeTagId>): Flow<List<TradeTag>> {
        return tradesDB.tradeTagQueries.getAllByIds(ids).asFlow().mapToList(appDispatchers.IO)
    }

    fun getSuggestedTags(
        ignoreIds: List<TradeTagId>,
        query: String,
    ): Flow<List<TradeTag>> {
        return tradesDB.tradeTagQueries.getSuggestedTags(ignoreIds, query).asFlow().mapToList(appDispatchers.IO)
    }

    fun getTagsForTrade(id: TradeId): Flow<List<TradeTag>> {
        return tradesDB.tradeToTagMapQueries.getTagsByTrade(id).asFlow().mapToList(appDispatchers.IO)
    }

    fun getSuggestedTagsForTrade(tradeId: TradeId, filter: String): Flow<List<TradeTag>> {
        return tradesDB.tradeToTagMapQueries
            .getSuggestedTagsForTrade(tradeId, filter)
            .asFlow()
            .mapToList(appDispatchers.IO)
    }

    suspend fun createTag(
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

    suspend fun updateTag(
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

    suspend fun deleteTag(id: TradeTagId) = withContext(appDispatchers.IO) {
        tradesDB.tradeTagQueries.delete(id)
    }

    suspend fun isTagNameUnique(
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

    suspend fun addTag(tradeId: TradeId, tagId: TradeTagId) = withContext(appDispatchers.IO) {

        tradesDB.tradeToTagMapQueries.insert(
            tradeId = tradeId,
            tagId = tagId,
        )
    }

    suspend fun removeTag(tradeId: TradeId, tagId: TradeTagId) = withContext(appDispatchers.IO) {

        tradesDB.tradeToTagMapQueries.delete(
            tradeId = tradeId,
            tagId = tagId,
        )
    }
}
