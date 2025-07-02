package com.saurabhsandav.trading.candledata.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.time.Instant

class CandlesQueries(
    driver: SqlDriver,
    private val tableName: String,
    private val identifierSeries: Int,
) : TransacterImpl(driver) {

    fun getCountInRange(
        from: Long,
        to: Long,
    ): Query<Long> = GetCountInRangeQuery(
        identifier = identifierSeries + Identifier_getCountInRange,
        from = from,
        to = to,
    ) { cursor ->
        cursor.getLong(0)!!
    }

    fun <T : Any> getInstantBeforeByCount(
        before: Long,
        count: Long,
        mapper: (epochSeconds: Long) -> T,
    ): Query<T> = GetInstantBeforeByCountQuery(
        identifier = identifierSeries + Identifier_getInstantBeforeByCount,
        before = before,
        count = count,
    ) { cursor ->
        mapper(cursor.getLong(0)!!)
    }

    fun <T : Any> getInstantAfterByCount(
        after: Long,
        count: Long,
        mapper: (epochSeconds: Long) -> T,
    ): Query<T> = GetInstantAfterByCountQuery(
        identifier = identifierSeries + Identifier_getInstantAfterByCount,
        after = after,
        count = count,
    ) { cursor ->
        mapper(cursor.getLong(0)!!)
    }

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

    fun <T : Any> getInRangeFromCandleInclusive(
        from: Long,
        to: Long,
        candleSeconds: Long,
        mapper: (
            epochSeconds: Long,
            open: String,
            high: String,
            low: String,
            close: String,
            volume: Long,
        ) -> T,
    ): Query<T> = GetInRangeFromCandleInclusiveQuery(
        identifier = identifierSeries + Identifier_getInRangeFromCandleInclusive,
        from = from,
        to = to,
        candleSeconds = candleSeconds,
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
        mapper: (
            beforeCount: Long,
            afterCount: Long,
            firstCandleEpochSeconds: Long?,
            lastCandleEpochSeconds: Long?,
            atCandleExists: Boolean,
        ) -> T,
    ): Query<T> = GetEpochSecondsAndCountAtQuery(
        identifier = identifierSeries + Identifier_getEpochSecondsAndCountAt,
        at = at,
    ) { cursor ->
        mapper(
            cursor.getLong(0)!!,
            cursor.getLong(1)!!,
            cursor.getLong(2),
            cursor.getLong(3),
            cursor.getBoolean(4)!!,
        )
    }

    fun getEpochSecondsAndCountAt(at: Long): Query<GetEpochSecondsAndCountAt> = getEpochSecondsAndCountAt(at) {
        beforeCount,
        afterCount,
        firstCandleEpochSeconds,
        lastCandleEpochSeconds,
        atCandleExists,
        ->
        GetEpochSecondsAndCountAt(
            beforeCount,
            afterCount,
            firstCandleEpochSeconds,
            lastCandleEpochSeconds,
            atCandleExists,
        )
    }

    fun <T : Any> getCountBefore(
        at: Long,
        includeAt: Boolean,
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
            cursor.getLong(5)!!,
        )
    }

    fun <T : Any> getCountAfter(
        at: Long,
        includeAt: Boolean,
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
            cursor.getLong(5)!!,
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

    fun delete(
        from: Instant,
        to: Instant,
    ) {

        val identifier = identifierSeries + Identifier_delete

        driver.execute(
            identifier = identifier,
            sql = """
                |DELETE FROM $tableName WHERE epochSeconds BETWEEN ? AND ?
            """.trimMargin(),
            parameters = 2,
        ) {

            bindLong(0, from.epochSeconds)
            bindLong(1, to.epochSeconds)
        }

        notifyQueries(identifier) { emit ->
            emit(tableName)
        }
    }

    private inner class GetCountInRangeQuery<out T : Any>(
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

        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(
            identifier = identifier,
            sql = """
                    |SELECT COUNT(*) FROM $tableName
                    |WHERE epochSeconds BETWEEN ? AND ?
            """.trimMargin(),
            mapper = mapper,
            parameters = 2,
        ) {
            bindLong(0, from)
            bindLong(1, to)
        }

        override fun toString(): String = "Candles.sq:getCountInRange"
    }

    private inner class GetInstantBeforeByCountQuery<out T : Any>(
        val identifier: Int,
        val before: Long,
        val count: Long,
        mapper: (SqlCursor) -> T,
    ) : Query<T>(mapper) {

        override fun addListener(listener: Listener) {
            driver.addListener(tableName, listener = listener)
        }

        override fun removeListener(listener: Listener) {
            driver.removeListener(tableName, listener = listener)
        }

        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(
            identifier = identifier,
            sql = """
                    |WITH candles AS (
                    |  SELECT epochSeconds FROM $tableName
                    |  WHERE epochSeconds < ?
                    |  ORDER BY epochSeconds DESC
                    |)
                    |SELECT * FROM candles
                    |LIMIT 1
                    |OFFSET MIN(?, (SELECT count(*) FROM candles)) - 1
            """.trimMargin(),
            mapper = mapper,
            parameters = 2,
        ) {
            bindLong(0, before)
            bindLong(1, count)
        }

        override fun toString(): String = "Candles.sq:getInstantBeforeByCount"
    }

    private inner class GetInstantAfterByCountQuery<out T : Any>(
        val identifier: Int,
        val after: Long,
        val count: Long,
        mapper: (SqlCursor) -> T,
    ) : Query<T>(mapper) {

        override fun addListener(listener: Listener) {
            driver.addListener(tableName, listener = listener)
        }

        override fun removeListener(listener: Listener) {
            driver.removeListener(tableName, listener = listener)
        }

        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(
            identifier = identifier,
            sql = """
                    |WITH candles AS (
                    |  SELECT epochSeconds FROM $tableName
                    |  WHERE epochSeconds > ?
                    |  ORDER BY epochSeconds ASC
                    |)
                    |SELECT * FROM candles
                    |LIMIT 1
                    |OFFSET MIN(?, (SELECT count(*) FROM candles)) - 1
            """.trimMargin(),
            mapper = mapper,
            parameters = 2,
        ) {
            bindLong(0, after)
            bindLong(1, count)
        }

        override fun toString(): String = "Candles.sq:getInstantAfterByCount"
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

        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(
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

    private inner class GetInRangeFromCandleInclusiveQuery<out T : Any>(
        val identifier: Int,
        val from: Long,
        val to: Long,
        val candleSeconds: Long,
        mapper: (SqlCursor) -> T,
    ) : Query<T>(mapper) {

        override fun addListener(listener: Listener) {
            driver.addListener(tableName, listener = listener)
        }

        override fun removeListener(listener: Listener) {
            driver.removeListener(tableName, listener = listener)
        }

        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(
            identifier = identifier,
            sql = """
                    |SELECT * FROM $tableName
                    |WHERE epochSeconds BETWEEN ? AND ?
                    |OR ? BETWEEN epochSeconds AND (epochSeconds + ?)
                    |ORDER BY epochSeconds
            """.trimMargin(),
            mapper = mapper,
            parameters = 4,
        ) {
            bindLong(0, from)
            bindLong(1, to)
            bindLong(2, from)
            bindLong(3, candleSeconds)
        }

        override fun toString(): String = "Candles.sq:getInRangeEdgeCandlesInclusive"
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

        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(
            identifier = identifier,
            sql = """
                |SELECT * FROM (
                |  SELECT COUNT(*) AS beforeCount FROM $tableName WHERE epochSeconds < ?
                |), (
                |  SELECT COUNT(*) AS afterCount FROM $tableName WHERE epochSeconds > ?
                |), (
                |  SELECT MIN(epochSeconds) AS firstCandleEpochSeconds, MAX(epochSeconds) AS lastCandleEpochSeconds FROM $tableName
                |), (
                |  SELECT EXISTS(
                |    SELECT * FROM $tableName
                |    WHERE epochSeconds = ?
                |  ) AS atCandleExists
                |);
            """.trimMargin(),
            mapper = mapper,
            parameters = 3,
        ) {
            bindLong(0, at)
            bindLong(1, at)
            bindLong(2, at)
        }

        override fun toString(): String = "Candles.sq:getEpochSecondsAndCountAt"
    }

    private inner class GetCountBeforeQuery<out T : Any>(
        val identifier: Int,
        val at: Long,
        val includeAt: Boolean,
        val count: Long,
        mapper: (SqlCursor) -> T,
    ) : Query<T>(mapper) {

        override fun addListener(listener: Listener) {
            driver.addListener(tableName, listener = listener)
        }

        override fun removeListener(listener: Listener) {
            driver.removeListener(tableName, listener = listener)
        }

        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(
            identifier = identifier,
            sql = """
                    |SELECT * FROM (
                    |  SELECT * FROM $tableName
                    |  WHERE epochSeconds < ? OR (? = TRUE AND epochSeconds = ?)
                    |  ORDER BY epochSeconds DESC
                    |  LIMIT ?
                    |)
                    |ORDER BY epochSeconds ASC
            """.trimMargin(),
            mapper = mapper,
            parameters = 4,
        ) {
            bindLong(0, at)
            bindBoolean(1, includeAt)
            bindLong(2, at)
            bindLong(3, count)
        }

        override fun toString(): String = "Candles.sq:getCountBefore"
    }

    private inner class GetCountAfterQuery<out T : Any>(
        val identifier: Int,
        val at: Long,
        val includeAt: Boolean,
        val count: Long,
        mapper: (SqlCursor) -> T,
    ) : Query<T>(mapper) {

        override fun addListener(listener: Listener) {
            driver.addListener(tableName, listener = listener)
        }

        override fun removeListener(listener: Listener) {
            driver.removeListener(tableName, listener = listener)
        }

        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(
            identifier = identifier,
            sql = """
                    |SELECT * FROM $tableName
                    |WHERE epochSeconds > ? OR (? = TRUE AND epochSeconds = ?)
                    |ORDER BY epochSeconds ASC
                    |LIMIT ?
            """.trimMargin(),
            mapper = mapper,
            parameters = 4,
        ) {
            bindLong(0, at)
            bindBoolean(1, includeAt)
            bindLong(2, at)
            bindLong(3, count)
        }

        override fun toString(): String = "Candles.sq:getCountAfter"
    }

    @Suppress("ktlint:standard:property-naming")
    companion object {

        private const val Identifier_insert = 1
        private const val Identifier_delete = 2
        private const val Identifier_getCountInRange = 3
        private const val Identifier_getInRange = 4
        private const val Identifier_getInRangeFromCandleInclusive = 5
        private const val Identifier_getEpochSecondsAndCountAt = 6
        private const val Identifier_getCountBefore = 7
        private const val Identifier_getCountAfter = 8
        private const val Identifier_getInstantBeforeByCount = 9
        private const val Identifier_getInstantAfterByCount = 10
    }
}

data class GetEpochSecondsAndCountAt(
    val beforeCount: Long,
    val afterCount: Long,
    val firstCandleEpochSeconds: Long?,
    val lastCandleEpochSeconds: Long?,
    val atCandleExists: Boolean,
)
