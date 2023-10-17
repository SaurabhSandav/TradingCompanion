package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.trades.model.*
import com.saurabhsandav.core.utils.withoutNanoseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.math.BigDecimal
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.*

internal class TradesRepo(
    recordPath: String,
    private val tradesDB: TradesDB,
    private val executionsRepo: TradeExecutionsRepo,
) {

    val attachmentsPath = Path(recordPath, AttachmentFolderName)

    val allTrades: Flow<List<Trade>>
        get() = tradesDB.tradeQueries.getAll().asFlow().mapToList(Dispatchers.IO)

    fun getById(id: TradeId): Flow<Trade> {
        return tradesDB.tradeQueries.getById(id).asFlow().mapToOne(Dispatchers.IO)
    }

    fun getByIds(ids: List<TradeId>): Flow<List<Trade>> {
        return tradesDB.tradeQueries.getByIds(ids).asFlow().mapToList(Dispatchers.IO)
    }

    fun getOpen(): Flow<List<Trade>> {
        return tradesDB.tradeQueries.getOpen().asFlow().mapToList(Dispatchers.IO)
    }

    fun getOpenCount(): Flow<Long> {
        return tradesDB.tradeQueries.getOpenCount().asFlow().mapToOne(Dispatchers.IO)
    }

    fun getToday(): Flow<List<Trade>> {
        return tradesDB.tradeQueries.getToday().asFlow().mapToList(Dispatchers.IO)
    }

    fun getBeforeToday(): Flow<List<Trade>> {
        return tradesDB.tradeQueries.getBeforeToday().asFlow().mapToList(Dispatchers.IO)
    }

    fun getByTickerInInterval(
        ticker: String,
        range: ClosedRange<Instant>,
    ): Flow<List<Trade>> {
        return tradesDB.tradeQueries
            .getByTickerInInterval(
                ticker = ticker,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
            )
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun getByTickerAndIdsInInterval(
        ticker: String,
        ids: List<TradeId>,
        range: ClosedRange<Instant>,
    ): Flow<List<Trade>> {
        return tradesDB.tradeQueries
            .getByTickerAndIdsInInterval(
                ticker = ticker,
                ids = ids,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
            )
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun getExecutionsForTrade(id: TradeId): Flow<List<TradeExecution>> {
        return executionsRepo.getExecutionsForTrade(id)
    }

    fun getTradesForExecution(executionId: TradeExecutionId): Flow<List<Trade>> {
        return tradesDB.tradeToExecutionMapQueries
            .getTradesByExecution(executionId)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun getWithoutMfeAndMaeBefore(instant: Instant): Flow<List<Trade>> {
        return tradesDB.tradeQueries
            .getWithoutMfeAndMaeBeforeTimestamp(instant)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    suspend fun setMfeAndMae(
        id: TradeId,
        mfePrice: BigDecimal,
        mfePnl: BigDecimal,
        maePrice: BigDecimal,
        maePnl: BigDecimal,
    ) = withContext(Dispatchers.IO) {

        // Save MFE and MAE
        tradesDB.tradeMfeMaeQueries.insert(
            tradeId = id,
            mfePrice = mfePrice,
            mfePnl = mfePnl,
            maePrice = maePrice,
            maePnl = maePnl,
        )
    }

    fun getMfeAndMae(id: TradeId): Flow<TradeMfeMae?> {
        return tradesDB.tradeMfeMaeQueries.getByTrade(id).asFlow().mapToOneOrNull(Dispatchers.IO)
    }

    fun getStopsForTrade(id: TradeId): Flow<List<TradeStop>> {
        return tradesDB.tradeStopQueries.getByTrade(id).asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun addStop(id: TradeId, price: BigDecimal) = withContext(Dispatchers.IO) {

        val trade = getById(id).first()

        val stopIsValid = when (trade.side) {
            TradeSide.Long -> price < trade.averageEntry
            TradeSide.Short -> price > trade.averageEntry
        }

        if (!stopIsValid) error("Invalid stop for Trade (#$id)")

        // Insert into DB
        tradesDB.tradeStopQueries.insert(
            tradeId = id,
            price = price,
        )
    }

    suspend fun deleteStop(id: TradeId, price: BigDecimal) = withContext(Dispatchers.IO) {
        tradesDB.tradeStopQueries.delete(tradeId = id, price = price)
    }

    fun getTargetsForTrade(id: TradeId): Flow<List<TradeTarget>> {
        return tradesDB.tradeTargetQueries.getByTrade(id).asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun addTarget(id: TradeId, price: BigDecimal) = withContext(Dispatchers.IO) {

        val trade = getById(id).first()

        val targetIsValid = when (trade.side) {
            TradeSide.Long -> price > trade.averageEntry
            TradeSide.Short -> price < trade.averageEntry
        }

        if (!targetIsValid) error("Invalid target for Trade (#$id)")

        // Insert into DB
        tradesDB.tradeTargetQueries.insert(
            tradeId = id,
            price = price,
        )
    }

    suspend fun deleteTarget(id: TradeId, price: BigDecimal) = withContext(Dispatchers.IO) {
        tradesDB.tradeTargetQueries.delete(tradeId = id, price = price)
    }

    fun getAllTags(): Flow<List<TradeTag>> {
        return tradesDB.tradeTagQueries.getAll().asFlow().mapToList(Dispatchers.IO)
    }

    fun getTagById(id: TradeTagId): Flow<TradeTag> {
        return tradesDB.tradeTagQueries.getById(id).asFlow().mapToOne(Dispatchers.IO)
    }

    fun getTagsForTrade(id: TradeId): Flow<List<TradeTag>> {
        return tradesDB.tradeToTagMapQueries.getTagsByTrade(id).asFlow().mapToList(Dispatchers.IO)
    }

    fun getSuggestedTagsForTrade(tradeId: TradeId, filter: String): Flow<List<TradeTag>> {
        return tradesDB.tradeToTagMapQueries
            .getSuggestedTagsForTrade(tradeId, filter)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    suspend fun createTag(name: String, description: String) = withContext(Dispatchers.IO) {

        tradesDB.tradeTagQueries.insert(
            name = name,
            description = description,
        )
    }

    suspend fun updateTag(
        id: TradeTagId,
        name: String,
        description: String,
    ) = withContext(Dispatchers.IO) {

        tradesDB.tradeTagQueries.update(
            id = id,
            name = name,
            description = description,
        )
    }

    suspend fun copyTag(id: TradeTagId) = withContext(Dispatchers.IO) {

        // Get tag details to copy
        val tag = tradesDB.tradeTagQueries.getById(id).executeAsOne()

        tradesDB.tradeTagQueries.insert(
            name = tag.name,
            description = tag.description,
        )
    }

    suspend fun deleteTag(id: TradeTagId) = withContext(Dispatchers.IO) {
        tradesDB.tradeTagQueries.delete(id)
    }

    suspend fun isTagNameUnique(
        name: String,
        ignoreTagId: TradeTagId? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext tradesDB.tradeTagQueries
            .run {
                when {
                    ignoreTagId == null -> isTagNameUnique(name)
                    else -> isTagNameUniqueIgnoreId(name, ignoreTagId)
                }
            }
            .executeAsOne()
    }

    suspend fun addTag(tradeId: TradeId, tagId: TradeTagId) = withContext(Dispatchers.IO) {

        tradesDB.tradeToTagMapQueries.insert(
            tradeId = tradeId,
            tagId = tagId,
        )
    }

    suspend fun removeTag(tradeId: TradeId, tagId: TradeTagId) = withContext(Dispatchers.IO) {

        tradesDB.tradeToTagMapQueries.delete(
            tradeId = tradeId,
            tagId = tagId,
        )
    }

    fun getAttachmentsForTrade(id: TradeId): Flow<List<GetAttachmentsByTrade>> {
        return tradesDB.tradeToAttachmentMapQueries.getAttachmentsByTrade(id).asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun addAttachment(
        tradeId: TradeId,
        name: String,
        description: String,
        pathStr: String,
    ) = withContext(Dispatchers.IO) {

        tradesDB.transaction {

            val inputFilepath = Path(pathStr)
            val checksum = generateSHA1(inputFilepath)

            // Get Attachment from DB if it exists
            val existingAttachment = tradesDB.tradeAttachmentQueries.getByChecksum(checksum).executeAsOneOrNull()

            val attachmentId = when {
                existingAttachment == null -> {

                    val attachedFileName = "$checksum.${inputFilepath.extension}"
                    val attachedFilePath = attachmentsPath.resolve(attachedFileName)

                    // Create attachment folder
                    attachmentsPath.createDirectories()

                    // Copy file to attachments directory
                    inputFilepath.copyTo(attachedFilePath)

                    // Save attachment entry to DB
                    tradesDB.tradeAttachmentQueries.insert(
                        fileName = attachedFileName,
                        checksum = checksum,
                    )

                    // Get id of attachment in DB
                    tradesDB.tradesDBUtilsQueries.lastInsertedRowId().executeAsOne().let(::TradeAttachmentId)
                }

                else -> existingAttachment.id
            }

            // Link Attachment to Trade
            tradesDB.tradeToAttachmentMapQueries.insert(
                tradeId = tradeId,
                attachmentId = attachmentId,
                name = name,
                description = description,
            )
        }
    }

    private fun generateSHA1(path: Path): String {

        val fileInputStream = path.inputStream()
        val digest = MessageDigest.getInstance("SHA-1")
        val digestInputStream = DigestInputStream(fileInputStream, digest)
        val bytes = ByteArray(1024)

        // read all file content
        @Suppress("ControlFlowWithEmptyBody")
        while (digestInputStream.read(bytes) > 0);

        val resultByteArray = digest.digest()

        return buildString {

            for (b in resultByteArray) {

                val value = b.toInt() and 0xFF

                if (value < 16) {
                    // if value less than 16, then it's hex String will be only
                    // one character, so we need to append a character of '0'
                    append("0")
                }

                append(Integer.toHexString(value).uppercase(Locale.getDefault()))
            }
        }
    }

    suspend fun updateAttachment(
        tradeId: TradeId,
        attachmentId: TradeAttachmentId,
        name: String,
        description: String,
    ) = withContext(Dispatchers.IO) {

        tradesDB.tradeToAttachmentMapQueries.update(
            tradeId = tradeId,
            attachmentId = attachmentId,
            name = name,
            description = description,
        )
    }

    suspend fun removeAttachment(
        tradeId: TradeId,
        attachmentId: TradeAttachmentId,
    ) = withContext(Dispatchers.IO) {

        tradesDB.transaction {

            // Delete attachment from trade in DB
            tradesDB.tradeToAttachmentMapQueries.delete(
                tradeId = tradeId,
                attachmentId = attachmentId,
            )

            // Check if attachment is still used
            val isAttachmentStillUsed = tradesDB.tradeToAttachmentMapQueries
                .isAttachmentLinked(attachmentId)
                .executeAsOne()

            // If attachment is not used, delete file
            if (!isAttachmentStillUsed) {

                val attachment = tradesDB.tradeAttachmentQueries.getById(attachmentId).executeAsOne()

                // Delete attachment DB entry
                tradesDB.tradeAttachmentQueries.delete(attachmentId)

                // Delete attachment file
                attachmentsPath.resolve(attachment.fileName).deleteExisting()
            }
        }
    }

    fun getNotesForTrade(id: TradeId): Flow<List<TradeNote>> {
        return tradesDB.tradeNoteQueries.getByTrade(id).asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun addNote(
        tradeId: TradeId,
        note: String,
        isMarkdown: Boolean,
    ) = withContext(Dispatchers.IO) {

        val now = Clock.System.now().withoutNanoseconds()

        tradesDB.tradeNoteQueries.insert(
            tradeId = tradeId,
            note = note,
            added = now,
            lastEdited = now,
            isMarkdown = isMarkdown,
        )
    }

    suspend fun updateNote(
        id: TradeNoteId,
        note: String,
        isMarkdown: Boolean,
    ) = withContext(Dispatchers.IO) {

        tradesDB.tradeNoteQueries.update(
            id = id,
            note = note,
            lastEdited = Clock.System.now().withoutNanoseconds(),
            isMarkdown = isMarkdown,
        )
    }

    suspend fun deleteNote(id: TradeNoteId) = withContext(Dispatchers.IO) {
        tradesDB.tradeNoteQueries.delete(id)
    }

    companion object {

        const val AttachmentFolderName = "attachments"
    }
}
