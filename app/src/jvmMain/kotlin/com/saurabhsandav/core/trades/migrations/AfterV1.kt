package com.saurabhsandav.core.trades.migrations

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion

val migrationAfterV1 = AfterVersion(1) { driver ->

    val transacter = object : TransacterImpl(driver) {}

    transacter.transaction {

        // Set farthest stop as primary
        driver.execute(
            identifier = null,
            sql = """
                |UPDATE TradeStop AS ts
                |SET isPrimary = TRUE 
                |WHERE price = (
                |  SELECT TradeStop.price
                |  FROM TradeStop
                |  INNER JOIN Trade ON TradeStop.tradeId = Trade.id
                |  WHERE Trade.id = ts.tradeId
                |  ORDER BY IIF(Trade.side = 'long', 1, -1.0) * CAST(TradeStop.price AS REAL)
                |  LIMIT 1
                |);
            """.trimMargin(),
            parameters = 0,
        )

        // Set closest target as primary
        driver.execute(
            identifier = null,
            sql = """
                |UPDATE TradeTarget AS tt
                |SET isPrimary = TRUE 
                |WHERE price = (
                |  SELECT TradeTarget.price
                |  FROM TradeTarget
                |  INNER JOIN Trade ON TradeTarget.tradeId = Trade.id
                |  WHERE Trade.id = tt.tradeId
                |  ORDER BY IIF(Trade.side = 'long', 1, -1.0) * CAST(TradeTarget.price AS REAL)
                |  LIMIT 1
                |);
            """.trimMargin(),
            parameters = 0,
        )
    }
}
