package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.saurabhsandav.core.trades.model.AttachmentFileId
import com.saurabhsandav.core.trades.model.AttachmentWithFile
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.utils.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Locale
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.extension
import kotlin.io.path.inputStream

class Attachments internal constructor(
    private val appDispatchers: AppDispatchers,
    private val tradesDB: TradesDB,
    private val attachmentsPath: Path,
) {

    fun getByIdWithFile(
        tradeId: TradeId,
        fileId: AttachmentFileId,
    ): Flow<AttachmentWithFile> {
        return tradesDB.tradeAttachmentQueries
            .getByIdWithFile(tradeId, fileId, ::toAttachmentWithFile)
            .asFlow()
            .mapToOne(appDispatchers.IO)
    }

    fun getForTradeWithFile(id: TradeId): Flow<List<AttachmentWithFile>> {
        return tradesDB.tradeAttachmentQueries
            .getByTradeWithFile(id, ::toAttachmentWithFile)
            .asFlow()
            .mapToList(appDispatchers.IO)
    }

    suspend fun add(
        tradeIds: List<TradeId>,
        name: String,
        description: String,
        pathStr: String,
    ) = withContext(appDispatchers.IO) {

        tradesDB.transaction {

            val inputFilepath = Path(pathStr)
            val checksum = generateSHA1(inputFilepath)

            // Get Attachment from DB if it exists
            val existingAttachment = tradesDB.attachmentFileQueries.getByChecksum(checksum).executeAsOneOrNull()

            val fileId = when {
                existingAttachment == null -> {

                    val attachedFileName = "$checksum.${inputFilepath.extension}"
                    val attachedFilePath = attachmentsPath.resolve(attachedFileName)

                    // Create attachment folder
                    attachmentsPath.createDirectories()

                    // Copy file to attachments directory
                    inputFilepath.copyTo(attachedFilePath)

                    // Save attachment entry to DB
                    tradesDB.attachmentFileQueries.insert(
                        fileName = attachedFileName,
                        checksum = checksum,
                    )

                    // Get id of attachment in DB
                    tradesDB.tradesDBUtilsQueries.lastInsertedRowId().executeAsOne().let(::AttachmentFileId)
                }

                else -> existingAttachment.id
            }

            // Link Attachment to Trades
            tradeIds.forEach { tradeId ->

                tradesDB.tradeAttachmentQueries.insert(
                    tradeId = tradeId,
                    fileId = fileId,
                    name = name,
                    description = description,
                )
            }
        }
    }

    suspend fun update(
        tradeId: TradeId,
        fileId: AttachmentFileId,
        name: String,
        description: String,
    ) = withContext(appDispatchers.IO) {

        tradesDB.tradeAttachmentQueries.update(
            tradeId = tradeId,
            fileId = fileId,
            name = name,
            description = description,
        )
    }

    suspend fun remove(
        tradeId: TradeId,
        fileId: AttachmentFileId,
    ) = withContext(appDispatchers.IO) {

        tradesDB.transaction {

            // Delete attachment from trade in DB
            tradesDB.tradeAttachmentQueries.delete(
                tradeId = tradeId,
                fileId = fileId,
            )

            // Check if attachment is still used
            val isAttachmentStillUsed = tradesDB.tradeAttachmentQueries
                .isAttachmentLinked(fileId)
                .executeAsOne()

            // If attachment is not used, delete file
            if (!isAttachmentStillUsed) {

                val attachment = tradesDB.attachmentFileQueries.getById(fileId).executeAsOne()

                // Delete attachment DB entry
                tradesDB.attachmentFileQueries.delete(fileId)

                // Delete attachment file
                attachmentsPath.resolve(attachment.fileName).deleteExisting()
            }
        }
    }

    private fun generateSHA1(path: Path): String {

        val fileInputStream = path.inputStream()
        val digest = MessageDigest.getInstance("SHA-1")
        val digestInputStream = DigestInputStream(fileInputStream, digest)
        val bytes = ByteArray(1024)

        // read all file content
        @Suppress("ControlFlowWithEmptyBody", "ktlint:standard:annotation")
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

    private fun toAttachmentWithFile(
        tradeId: TradeId,
        fileId: AttachmentFileId,
        name: String,
        description: String,
        fileName: String,
        checksum: String,
    ) = AttachmentWithFile(
        tradeId = tradeId,
        fileId = fileId,
        name = name,
        description = description,
        path = attachmentsPath.resolve(fileName),
        checksum = checksum,
    )
}
