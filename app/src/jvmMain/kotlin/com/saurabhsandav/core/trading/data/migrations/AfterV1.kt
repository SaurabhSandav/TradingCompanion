package com.saurabhsandav.core.trading.data.migrations

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult

val migrationAfterV1 = AfterVersion(1) { driver ->

    val transacter = object : TransacterImpl(driver) {}

    transacter.transaction {

        data class Table(
            val currentName: String,
            val newName: String,
        )

        val tables = driver.executeQuery(
            identifier = null,
            sql = """
                |SELECT name FROM sqlite_schema
                |WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'CheckedRange';
            """.trimMargin(),
            parameters = 0,
            mapper = { cursor ->

                val tables = buildList {

                    while (cursor.next().value) {

                        val currentName = cursor.getString(0)!!
                        val newName = buildString {
                            append("NSE_")
                            val components = currentName.split('_').filter { it != "" }
                            val name = components.dropLast(1).joinToString("_")
                            append(name)
                            append(if (name == "NIFTY50") "_INDEX_" else "_EQ_")
                            append(components.last())
                        }

                        val idAndFileName = Table(
                            currentName = currentName,
                            newName = newName,
                        )

                        add(idAndFileName)
                    }
                }

                QueryResult.Value(tables)
            },
        ).value

        tables.forEach { (currentName, newName) ->

            driver.execute(
                identifier = null,
                sql = "ALTER TABLE $currentName RENAME TO $newName",
                parameters = 0,
            )

            driver.execute(
                identifier = null,
                sql = "UPDATE CheckedRange SET tableName = ? WHERE tableName = ?",
                parameters = 0,
                binders = {
                    bindString(0, newName)
                    bindString(1, currentName)
                },
            )
        }
    }
}
