package com.saurabhsandav.core.trading.data.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver

class CandlesQueries(
    driver: SqlDriver,
    private val tableName: String,
    private val identifierSeries: Int,
) : TransacterImpl(driver) {

    fun <T : Any> getInRange(
        from: Long,
        to: Long,
        mapper: (
            epochSeconds: Long,
            open: String,
            high: String,
            low: String,
            close: String,
            volume: Long,
        ) -> T,
    ): Query<T> = GetInRangeQuery(
        identifier = identifierSeries + Identifier_getInRange,
        from = from,
        to = to,
    ) { cursor ->

        mapper(
            cursor.getLong(0)!!,
            cursor.getString(1)!!,
            cursor.getString(2)!!,
            cursor.getString(3)!!,
            cursor.getString(4)!!,
            cursor.getLong(5)!!,
        )
    }

    private fun <T : Any> getEpochSecondsAndCountAt(
        at: Long,
        mapper: (MIN: Long?, COUNT: Long) -> T,
    ): Query<T> = GetEpochSecondsAndCountAtQuery(
        identifier = identifierSeries + Identifier_getEpochSecondsAndCountAt,
        at = at,
    ) { cursor ->

        mapper(
            cursor.getLong(0),
            cursor.getLong(1)!!,
        )
    }

    fun getEpochSecondsAndCountAt(
        at: Long,
    ): Query<GetEpochSecondsAndCountAt> = getEpochSecondsAndCountAt(at) { MIN, COUNT ->
        GetEpochSecondsAndCountAt(
            MIN,
            COUNT,
        )
    }

    fun <T : Any> getCountBefore(
        at: Long,
        includeAt: String?,
        count: Long,
        mapper: (
            epochSeconds: Long,
            open: String,
            high: String,
            low: String,
            close: String,
            volume: Long,
        ) -> T,
    ): Query<T> = GetCountBeforeQuery(
        identifier = identifierSeries + Identifier_getCountBefore,
        at = at,
        includeAt = includeAt,
        count = count,
    ) { cursor ->

        mapper(
            cursor.getLong(0)!!,
            cursor.getString(1)!!,
            cursor.getString(2)!!,
            cursor.getString(3)!!,
            cursor.getString(4)!!,
            cursor.getLong(5)!!
        )
    }

    fun <T : Any> getCountAfter(
        at: Long,
        includeAt: String?,
        count: Long,
        mapper: (
            epochSeconds: Long,
            open: String,
            high: String,
            low: String,
            close: String,
            volume: Long,
        ) -> T,
    ): Query<T> = GetCountAfterQuery(
        identifier = identifierSeries + Identifier_getCountAfter,
        at = at,
        includeAt = includeAt,
        count = count,
    ) { cursor ->

        mapper(
            cursor.getLong(0)!!,
            cursor.getString(1)!!,
            cursor.getString(2)!!,
            cursor.getString(3)!!,
            cursor.getString(4)!!,
            cursor.getLong(5)!!
        )
    }

    fun insert(
        epochSeconds: Long?,
        open: String,
        high: String,
        low: String,
        close: String,
        volume: Long,
    ) {

        val identifier = identifierSeries + Identifier_insert

        driver.execute(
            identifier = identifier,
            sql = """
                |INSERT OR REPLACE INTO $tableName
                |VALUES (?, ?, ?, ?, ?, ?)
                """.trimMargin(),
            parameters = 6,
        ) {

            bindLong(0, epochSeconds)
            bindString(1, open)
            bindString(2, high)
            bindString(3, low)
            bindString(4, close)
            bindLong(5, volume)
        }

        notifyQueries(identifier) { emit ->
            emit(tableName)
        }
    }

    private inner class GetInRangeQuery<out T : Any>(
        val identifier: Int,
        val from: Long,
        val to: Long,
        mapper: (SqlCursor) -> T,
    ) : Query<T>(mapper) {

        override fun addListener(listener: Listener) {
            driver.addListener(tableName, listener = listener)
        }

        override fun removeListener(listener: Listener) {
            driver.removeListener(tableName, listener = listener)
        }

        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
            driver.executeQuery(
                identifier = identifier,
                sql = """
                    |SELECT * FROM $tableName
                    |WHERE epochSeconds BETWEEN ? AND ?
                    |ORDER BY epochSeconds
                    """.trimMargin(),
                mapper = mapper,
                parameters = 2,
            ) {
                bindLong(0, from)
                bindLong(1, to)
            }

        override fun toString(): String = "Candles.sq:getInRange"
    }

    private inner class GetEpochSecondsAndCountAtQuery<out T : Any>(
        val identifier: Int,
        val at: Long,
        mapper: (SqlCursor) -> T,
    ) : Query<T>(mapper) {

        override fun addListener(listener: Listener) {
            driver.addListener(tableName, listener = listener)
        }

        override fun removeListener(listener: Listener) {
            driver.removeListener(tableName, listener = listener)
        }

        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
            driver.executeQuery(
                identifier = identifier,
                sql = """
                    |SELECT * FROM (
                    |  SELECT MIN(epochSeconds), COUNT(*) FROM $tableName
                    |  WHERE epochSeconds <= ?
                    |)
                    |UNION ALL
                    |SELECT * FROM (
                    |  SELECT MAX(epochSeconds), COUNT(*) FROM $tableName
                    |  WHERE epochSeconds > ?
                    |)
                    """.trimMargin(),
                mapper = mapper,
                parameters = 2,
            ) {
                bindLong(0, at)
                bindLong(1, at)
            }

        override fun toString(): String = "Candles.sq:getEpochSecondsAndCountAt"
    }

    private inner class GetCountBeforeQuery<out T : Any>(
        val identifier: Int,
        val at: Long,
        val includeAt: String?,
        val count: Long,
        mapper: (SqlCursor) -> T,
    ) : Query<T>(mapper) {

        override fun addListener(listener: Listener) {
            driver.addListener(tableName, listener = listener)
        }

        override fun removeListener(listener: Listener) {
            driver.removeListener(tableName, listener = listener)
        }

        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
            driver.executeQuery(
                identifier = identifier,
                sql = """
                    |SELECT * FROM (
                    |  SELECT * FROM $tableName
                    |  WHERE epochSeconds < ? OR (LOWER(?) = 'true' AND epochSeconds = ?)
                    |  ORDER BY epochSeconds DESC
                    |  LIMIT ?
                    |)
                    |ORDER BY epochSeconds ASC
                    """.trimMargin(),
                mapper = mapper,
                parameters = 4,
            ) {
                bindLong(0, at)
                bindString(1, includeAt)
                bindLong(2, at)
                bindLong(3, count)
            }

        override fun toString(): String = "Candles.sq:getCountBefore"
    }

    private inner class GetCountAfterQuery<out T : Any>(
        val identifier: Int,
        val at: Long,
        val includeAt: String?,
        val count: Long,
        mapper: (SqlCursor) -> T,
    ) : Query<T>(mapper) {

        override fun addListener(listener: Listener) {
            driver.addListener(tableName, listener = listener)
        }

        override fun removeListener(listener: Listener) {
            driver.removeListener(tableName, listener = listener)
        }

        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
            driver.executeQuery(
                identifier = identifier,
                sql = """
                    |SELECT * FROM $tableName
                    |WHERE epochSeconds > ? OR (LOWER(?) = 'true' AND epochSeconds = ?)
                    |ORDER BY epochSeconds ASC
                    |LIMIT ?
                    """.trimMargin(),
                mapper = mapper,
                parameters = 4,
            ) {
                bindLong(0, at)
                bindString(1, includeAt)
                bindLong(2, at)
                bindLong(3, count)
            }

        override fun toString(): String = "Candles.sq:getCountAfter"
    }

    companion object {

        private const val Identifier_insert = 1
        private const val Identifier_getInRange = 2
        private const val Identifier_getEpochSecondsAndCountAt = 3
        private const val Identifier_getCountBefore = 4
        private const val Identifier_getCountAfter = 5
    }
}

data class GetEpochSecondsAndCountAt(
    val MIN: Long?,
    val COUNT: Long,
)
