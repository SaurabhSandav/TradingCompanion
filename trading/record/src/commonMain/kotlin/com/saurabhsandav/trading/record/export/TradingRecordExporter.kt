package com.saurabhsandav.trading.record.export

import com.saurabhsandav.trading.record.TradesDB
import com.saurabhsandav.trading.record.TradingRecord

class TradingRecordExporter {

    fun export(record: TradingRecord) {

        val tradesDB = record.tradesDB

        with(tradesDB) {

            ExportTradingRecord(
                executions = exportExecutions(),
                trades = exportTrades(),
                stops = exportStops(),
                targets = exportTargets(),
                notes = exportNotes(),
                attachments = exportAttachments(),
                excursions = exportExcursions(),
                tag = exportTags(),
                reviews = exportReviews(),
                attachmentFiles = exportAttachmentFiles(),
                brokers = exportBrokers(),
                symbols = exportSymbols(),
                sizingTrades = exportSizingTrades(),
                tradeToExecutionMap = exportTradeToExecutionMap(),
                tradeToTagMap = exportTradeToTagMap(),
            )
        }
    }

    private fun TradesDB.exportExecutions(): List<ExportTradeExecution> {

        return tradeExecutionQueries.getAll().executeAsList().map { execution ->

            ExportTradeExecution(
                id = execution.id,
                brokerId = execution.brokerId,
                instrument = execution.instrument,
                symbolId = execution.symbolId,
                quantity = execution.quantity,
                lots = execution.lots,
                side = execution.side,
                price = execution.price,
                timestamp = execution.timestamp,
                locked = execution.locked,
            )
        }
    }

    private fun TradesDB.exportTrades(): List<ExportTrade> {

        return tradeQueries.getAll().executeAsList().map { trade ->

            ExportTrade(
                id = trade.id,
                brokerId = trade.brokerId,
                symbolId = trade.symbolId,
                instrument = trade.instrument,
                quantity = trade.quantity,
                closedQuantity = trade.closedQuantity,
                lots = trade.lots,
                side = trade.side,
                averageEntry = trade.averageEntry,
                entryTimestamp = trade.entryTimestamp,
                averageExit = trade.averageExit,
                exitTimestamp = trade.exitTimestamp,
                pnl = trade.pnl,
                fees = trade.fees,
                netPnl = trade.netPnl,
                isClosed = trade.isClosed,
            )
        }
    }

    private fun TradesDB.exportStops(): List<ExportTradeStop> {

        return tradeStopQueries.getAll().executeAsList().map { stop ->

            ExportTradeStop(
                tradeId = stop.tradeId,
                price = stop.price,
                isPrimary = stop.isPrimary,
            )
        }
    }

    private fun TradesDB.exportTargets(): List<ExportTradeTarget> {

        return tradeTargetQueries.getAll().executeAsList().map { target ->

            ExportTradeTarget(
                tradeId = target.tradeId,
                price = target.price,
                isPrimary = target.isPrimary,
            )
        }
    }

    private fun TradesDB.exportNotes(): List<ExportTradeNote> {

        return tradeNoteQueries.getAll().executeAsList().map { note ->

            ExportTradeNote(
                tradeId = note.tradeId,
                id = note.id,
                note = note.note,
                added = note.added,
                lastEdited = note.lastEdited,
            )
        }
    }

    private fun TradesDB.exportAttachments(): List<ExportTradeAttachment> {

        return tradeAttachmentQueries.getAll().executeAsList().map { attachment ->

            ExportTradeAttachment(
                tradeId = attachment.tradeId,
                fileId = attachment.fileId,
                name = attachment.name,
                description = attachment.description,
            )
        }
    }

    private fun TradesDB.exportExcursions(): List<ExportTradeExcursions> {

        return tradeExcursionsQueries.getAll().executeAsList().map { excursions ->

            ExportTradeExcursions(
                tradeId = excursions.tradeId,
                tradeMfePrice = excursions.tradeMfePrice,
                tradeMfePnl = excursions.tradeMfePnl,
                tradeMaePrice = excursions.tradeMaePrice,
                tradeMaePnl = excursions.tradeMaePnl,
                sessionMfePrice = excursions.sessionMfePrice,
                sessionMfePnl = excursions.sessionMfePnl,
                sessionMaePrice = excursions.sessionMaePrice,
                sessionMaePnl = excursions.sessionMaePnl,
            )
        }
    }

    private fun TradesDB.exportTags(): List<ExportTradeTag> {

        return tradeTagQueries.getAll().executeAsList().map { tag ->

            ExportTradeTag(
                id = tag.id,
                name = tag.name,
                description = tag.description,
                color = tag.color,
            )
        }
    }

    private fun TradesDB.exportReviews(): List<ExportReview> {

        return reviewQueries.getAll().executeAsList().map { review ->

            ExportReview(
                id = review.id,
                title = review.title,
                tradeIds = review.tradeIds,
                review = review.review,
                created = review.created,
                isPinned = review.isPinned,
            )
        }
    }

    private fun TradesDB.exportAttachmentFiles(): List<ExportAttachmentFile> {

        return attachmentFileQueries.getAll().executeAsList().map { attachmentFile ->

            ExportAttachmentFile(
                id = attachmentFile.id,
                fileName = attachmentFile.fileName,
                checksum = attachmentFile.checksum,
                mimeType = attachmentFile.mimeType,
            )
        }
    }

    private fun TradesDB.exportBrokers(): List<ExportBroker> {

        return brokerQueries.getAll().executeAsList().map { broker ->

            ExportBroker(
                id = broker.id,
                name = broker.name,
            )
        }
    }

    private fun TradesDB.exportSymbols(): List<ExportSymbol> {

        return symbolQueries.getAll().executeAsList().map { symbol ->

            ExportSymbol(
                id = symbol.id,
                brokerId = symbol.brokerId,
                instrument = symbol.instrument,
                exchange = symbol.exchange,
                ticker = symbol.ticker,
                description = symbol.description,
            )
        }
    }

    private fun TradesDB.exportSizingTrades(): List<ExportSizingTrade> {

        return sizingTradeQueries.getAll().executeAsList().map { sizingTrade ->

            ExportSizingTrade(
                id = sizingTrade.id,
                brokerId = sizingTrade.brokerId,
                symbolId = sizingTrade.symbolId,
                entry = sizingTrade.entry,
                stop = sizingTrade.stop,
            )
        }
    }

    private fun TradesDB.exportTradeToExecutionMap(): List<ExportTradeToExecutionMap> {

        return tradeToExecutionMapQueries.getAll().executeAsList().map { tradeToExecutionMap ->

            ExportTradeToExecutionMap(
                tradeId = tradeToExecutionMap.tradeId,
                executionId = tradeToExecutionMap.executionId,
                overrideQuantity = tradeToExecutionMap.overrideQuantity,
            )
        }
    }

    private fun TradesDB.exportTradeToTagMap(): List<ExportTradeToTagMap> {

        return tradeToTagMapQueries.getAll().executeAsList().map { tradeToTagMap ->

            ExportTradeToTagMap(
                tradeId = tradeToTagMap.tradeId,
                tagId = tradeToTagMap.tagId,
            )
        }
    }
}
