package com.saurabhsandav.trading.record.migrations

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import com.saurabhsandav.trading.record.utils.InstantReadableColumnAdapter
import com.saurabhsandav.trading.record.utils.withoutNanoseconds
import kotlin.time.Clock

val migrationAfterV2 = AfterVersion(2) { driver ->

    val transacter = object : TransacterImpl(driver) {}

    transacter.transaction {

        val reviewIdsWithEmptyCreated = driver.executeQuery(
            identifier = null,
            sql = "SELECT id FROM Review WHERE created = \"\"",
            parameters = 0,
            mapper = { cursor ->
                val result = mutableListOf<Long>()
                while (cursor.next().value) result.add(cursor.getLong(0)!!)
                QueryResult.Value(result)
            },
        ).value

        reviewIdsWithEmptyCreated.forEach { id ->

            val createdAt = InstantReadableColumnAdapter.encode(Clock.System.now().withoutNanoseconds())

            driver.execute(
                identifier = null,
                sql = "UPDATE Review SET created = ? WHERE id = ?;",
                parameters = 2,
                binders = {
                    bindString(0, createdAt)
                    bindLong(1, id)
                },
            )
        }
    }
}
