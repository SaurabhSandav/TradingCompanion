package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.saurabhsandav.core.trades.model.TradeAttachmentId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.utils.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.*

class Attachments internal constructor(
    private val appDispatchers: AppDispatchers,
    private val tradesDB: TradesDB,
    recordPath: Path,
) {

    val attachmentsPath: Path = recordPath.resolve("attachments")

    fun getForTrade(id: TradeId): Flow<List<GetAttachmentsByTrade>> {
        return tradesDB.tradeToAttachmentMapQueries.getAttachmentsByTrade(id).asFlow().mapToList(appDispatchers.IO)
    }

    suspend fun add(
        tradeId: TradeId,
        name: String,
        description: String,
        pathStr: String,
    ) = withContext(appDispatchers.IO) {

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

    suspend fun update(
        tradeId: TradeId,
        attachmentId: TradeAttachmentId,
        name: String,
        description: String,
    ) = withContext(appDispatchers.IO) {

        tradesDB.tradeToAttachmentMapQueries.update(
            tradeId = tradeId,
            attachmentId = attachmentId,
            name = name,
            description = description,
        )
    }

    suspend fun remove(
        tradeId: TradeId,
        attachmentId: TradeAttachmentId,
    ) = withContext(appDispatchers.IO) {

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
}
