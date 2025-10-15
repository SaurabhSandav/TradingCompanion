package com.saurabhsandav.trading.record.export

import com.saurabhsandav.trading.record.TradingRecord

class TradingRecordExport(
    record: TradingRecord,
) {

    private val tradesDB = record.tradesDB

    private fun exportExecutions(): List<ExportTradeExecution> {

        return tradesDB.tradeExecutionQueries.getAll().executeAsList().map { execution ->

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

    private fun exportTrades(): List<ExportTrade> {

        return tradesDB.tradeQueries.getAll().executeAsList().map { trade ->

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

    private fun exportStops(): List<ExportTradeStop> {

        return tradesDB.tradeStopQueries.getAll().executeAsList().map { stop ->

            ExportTradeStop(
                tradeId = stop.tradeId,
                price = stop.price,
                isPrimary = stop.isPrimary,
            )
        }
    }

    private fun exportTargets(): List<ExportTradeTarget> {

        return tradesDB.tradeTargetQueries.getAll().executeAsList().map { target ->

            ExportTradeTarget(
                tradeId = target.tradeId,
                price = target.price,
                isPrimary = target.isPrimary,
            )
        }
    }

    private fun exportNotes(): List<ExportTradeNote> {

        return tradesDB.tradeNoteQueries.getAll().executeAsList().map { note ->

            ExportTradeNote(
                tradeId = note.tradeId,
                id = note.id,
                note = note.note,
                added = note.added,
                lastEdited = note.lastEdited,
            )
        }
    }

    private fun exportAttachments(): List<ExportTradeAttachment> {

        return tradesDB.tradeAttachmentQueries.getAll().executeAsList().map { attachment ->

            ExportTradeAttachment(
                tradeId = attachment.tradeId,
                fileId = attachment.fileId,
                name = attachment.name,
                description = attachment.description,
            )
        }
    }

    private fun exportExcursions(): List<ExportTradeExcursions> {

        return tradesDB.tradeExcursionsQueries.getAll().executeAsList().map { excursions ->

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

    private fun exportTags(): List<ExportTradeTag> {

        return tradesDB.tradeTagQueries.getAll().executeAsList().map { tag ->

            ExportTradeTag(
                id = tag.id,
                name = tag.name,
                description = tag.description,
                color = tag.color,
            )
        }
    }

    private fun exportReviews(): List<ExportReview> {

        return tradesDB.reviewQueries.getAll().executeAsList().map { review ->

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

    private fun exportAttachmentFiles(): List<ExportAttachmentFile> {

        return tradesDB.attachmentFileQueries.getAll().executeAsList().map { attachmentFile ->

            ExportAttachmentFile(
                id = attachmentFile.id,
                fileName = attachmentFile.fileName,
                checksum = attachmentFile.checksum,
                mimeType = attachmentFile.mimeType,
            )
        }
    }

    private fun exportBrokers(): List<ExportBroker> {

        return tradesDB.brokerQueries.getAll().executeAsList().map { broker ->

            ExportBroker(
                id = broker.id,
                name = broker.name,
            )
        }
    }

    private fun exportSymbols(): List<ExportSymbol> {

        return tradesDB.symbolQueries.getAll().executeAsList().map { symbol ->

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

    private fun exportSizingTrades(): List<ExportSizingTrade> {

        return tradesDB.sizingTradeQueries.getAll().executeAsList().map { sizingTrade ->

            ExportSizingTrade(
                id = sizingTrade.id,
                brokerId = sizingTrade.brokerId,
                symbolId = sizingTrade.symbolId,
                entry = sizingTrade.entry,
                stop = sizingTrade.stop,
            )
        }
    }

    private fun exportTradeToExecutionMap(): List<ExportTradeToExecutionMap> {

        return tradesDB.tradeToExecutionMapQueries.getAll().executeAsList().map { tradeToExecutionMap ->

            ExportTradeToExecutionMap(
                tradeId = tradeToExecutionMap.tradeId,
                executionId = tradeToExecutionMap.executionId,
                overrideQuantity = tradeToExecutionMap.overrideQuantity,
            )
        }
    }

    private fun exportTradeToTagMap(): List<ExportTradeToTagMap> {

        return tradesDB.tradeToTagMapQueries.getAll().executeAsList().map { tradeToTagMap ->

            ExportTradeToTagMap(
                tradeId = tradeToTagMap.tradeId,
                tagId = tradeToTagMap.tagId,
            )
        }
    }
}
