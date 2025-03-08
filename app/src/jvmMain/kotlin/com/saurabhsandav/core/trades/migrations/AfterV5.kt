package com.saurabhsandav.core.trades.migrations

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import org.apache.tika.Tika
import java.nio.file.Path

// New mimeType column added to AttachmentFile table. Detect and update attachment MIME type in table.
fun migrationAfterV5(attachmentsPath: Path): AfterVersion = AfterVersion(5) { driver ->

    val transacter = object : TransacterImpl(driver) {}

    transacter.transaction {

        class IdAndFileName(
            val id: Long,
            val fileName: String,
        )

        val idAndFileNames = driver.executeQuery(
            identifier = null,
            sql = "SELECT id, fileName FROM AttachmentFile WHERE mimeType IS NULL;",
            parameters = 0,
            mapper = { cursor ->

                val result = mutableListOf<IdAndFileName>()

                while (cursor.next().value) {

                    val idAndFileName = IdAndFileName(
                        id = cursor.getLong(0)!!,
                        fileName = cursor.getString(1)!!,
                    )

                    result.add(idAndFileName)
                }

                QueryResult.Value(result)
            },
        ).value

        val tika = Tika()

        idAndFileNames.forEach { idAndFileName ->

            val mimeType = tika.detect(attachmentsPath.resolve(idAndFileName.fileName))

            driver.execute(
                identifier = null,
                sql = "UPDATE AttachmentFile SET mimeType = ? WHERE id = ?;",
                parameters = 2,
                binders = {
                    bindString(0, mimeType)
                    bindLong(1, idAndFileName.id)
                },
            )
        }
    }
}
