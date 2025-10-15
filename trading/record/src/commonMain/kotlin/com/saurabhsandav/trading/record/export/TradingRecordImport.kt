package com.saurabhsandav.trading.record.export

import com.saurabhsandav.trading.record.TradingRecord

interface TradingRecordImport {

    fun importExecutions(): List<ExportTradeExecution>

    fun importTrades(): List<ExportTrade>

    fun importStops(): List<ExportTradeStop>

    fun importTargets(): List<ExportTradeTarget>

    fun importNotes(): List<ExportTradeNote>

    fun importAttachments(): List<ExportTradeAttachment>

    fun importExcursions(): List<ExportTradeExcursions>

    fun importTags(): List<ExportTradeTag>

    fun importReviews(): List<ExportReview>

    fun importAttachmentFiles(): List<ExportAttachmentFile>

    fun importBrokers(): List<ExportBroker>

    fun importSymbols(): List<ExportSymbol>

    fun importSizingTrades(): List<ExportSizingTrade>

    fun importTradeToExecutionMap(): List<ExportTradeToExecutionMap>

    fun importTradeToTagMap(): List<ExportTradeToTagMap>
}

fun TradingRecord.import(importer: TradingRecordImport): Unit = tradesDB.transaction {

    // Executions
    importer.importExecutions().forEach { exportExecution ->

        tradesDB.tradeExecutionQueries.insert(
            id = exportExecution.id,
            brokerId = exportExecution.brokerId,
            instrument = exportExecution.instrument,
            symbolId = exportExecution.symbolId,
            quantity = exportExecution.quantity,
            lots = exportExecution.lots,
            side = exportExecution.side,
            price = exportExecution.price,
            timestamp = exportExecution.timestamp,
            locked = exportExecution.locked,
        )
    }

    // Trades
    importer.importTrades().forEach { exportTrade ->

        tradesDB.tradeQueries.insert(
            id = exportTrade.id,
            brokerId = exportTrade.brokerId,
            instrument = exportTrade.instrument,
            symbolId = exportTrade.symbolId,
            quantity = exportTrade.quantity,
            lots = exportTrade.lots,
            side = exportTrade.side,
            closedQuantity = exportTrade.closedQuantity,
            averageEntry = exportTrade.averageEntry,
            entryTimestamp = exportTrade.entryTimestamp,
            averageExit = exportTrade.averageExit,
            exitTimestamp = exportTrade.exitTimestamp,
            pnl = exportTrade.pnl,
            fees = exportTrade.fees,
            netPnl = exportTrade.netPnl,
            isClosed = exportTrade.isClosed,
        )
    }

/*    // Stops
    importer.importStops().forEach { exportStop ->

        tradesDB.tradeStopQueries.insert(
            tradeId = exportStop.tradeId,
            price = exportStop.price,
        )
    }

    // Targets
    importer.importTargets().forEach { exportTarget ->

        tradesDB.tradeTargetQueries.insert(
            tradeId = exportTarget.tradeId,
            price = exportTarget.price,
        )
    }*/

    // Notes
    importer.importNotes().forEach { exportNote ->

        tradesDB.tradeNoteQueries.insert(
            tradeId = exportNote.tradeId,
            note = exportNote.note,
            added = exportNote.added,
            lastEdited = exportNote.lastEdited,
        )
    }

    // Attachments
    importer.importAttachments().forEach { exportAttachment ->

        tradesDB.tradeAttachmentQueries.insert(
            tradeId = exportAttachment.tradeId,
            fileId = exportAttachment.fileId,
            name = exportAttachment.name,
            description = exportAttachment.description
        )
    }

    // Excursions
    importer.importExcursions().forEach { exportExcursions ->

        tradesDB.tradeExcursionsQueries.insert(
            tradeId = exportExcursions.tradeId,
            tradeMfePrice = exportExcursions.tradeMfePrice,
            tradeMfePnl = exportExcursions.tradeMfePnl,
            tradeMaePrice = exportExcursions.tradeMaePrice,
            tradeMaePnl = exportExcursions.tradeMaePnl,
            sessionMfePrice = exportExcursions.sessionMfePrice,
            sessionMfePnl = exportExcursions.sessionMfePnl,
            sessionMaePrice = exportExcursions.sessionMaePrice,
            sessionMaePnl = exportExcursions.sessionMaePnl
        )
    }

    // Sizing Trades
    importer.importSizingTrades().forEach { exportSizingTrade ->

        tradesDB.sizingTradeQueries.insert(
            brokerId = exportSizingTrade.brokerId,
            symbolId = exportSizingTrade.symbolId,
            entry = exportSizingTrade.entry,
            stop = exportSizingTrade.stop,
        )
    }
}
