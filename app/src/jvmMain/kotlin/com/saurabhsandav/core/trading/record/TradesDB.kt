package com.saurabhsandav.core.trading.record

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.saurabhsandav.core.trading.record.model.AttachmentFileIdColumnAdapter
import com.saurabhsandav.core.trading.record.model.Instrument
import com.saurabhsandav.core.trading.record.model.ReviewIdColumnAdapter
import com.saurabhsandav.core.trading.record.model.SizingTradeIdColumnAdapter
import com.saurabhsandav.core.trading.record.model.TradeExecutionIdColumnAdapter
import com.saurabhsandav.core.trading.record.model.TradeExecutionSide
import com.saurabhsandav.core.trading.record.model.TradeIdColumnAdapter
import com.saurabhsandav.core.trading.record.model.TradeIdListColumnAdapter
import com.saurabhsandav.core.trading.record.model.TradeNoteIdColumnAdapter
import com.saurabhsandav.core.trading.record.model.TradeSide
import com.saurabhsandav.core.trading.record.model.TradeTagIdColumnAdapter
import com.saurabhsandav.core.utils.BigDecimalColumnAdapter
import com.saurabhsandav.core.utils.InstantReadableColumnAdapter

internal fun TradesDB(driver: SqlDriver): TradesDB = TradesDB(
    driver = driver,
    SizingTradeAdapter = SizingTrade.Adapter(
        idAdapter = SizingTradeIdColumnAdapter,
        entryAdapter = BigDecimalColumnAdapter,
        stopAdapter = BigDecimalColumnAdapter,
    ),
    TradeAdapter = Trade.Adapter(
        idAdapter = TradeIdColumnAdapter,
        instrumentAdapter = Instrument.ColumnAdapter,
        quantityAdapter = BigDecimalColumnAdapter,
        closedQuantityAdapter = BigDecimalColumnAdapter,
        lotsAdapter = IntColumnAdapter,
        sideAdapter = TradeSide.ColumnAdapter,
        averageEntryAdapter = BigDecimalColumnAdapter,
        entryTimestampAdapter = InstantReadableColumnAdapter,
        averageExitAdapter = BigDecimalColumnAdapter,
        exitTimestampAdapter = InstantReadableColumnAdapter,
        pnlAdapter = BigDecimalColumnAdapter,
        feesAdapter = BigDecimalColumnAdapter,
        netPnlAdapter = BigDecimalColumnAdapter,
    ),
    TradeExecutionAdapter = TradeExecution.Adapter(
        idAdapter = TradeExecutionIdColumnAdapter,
        instrumentAdapter = Instrument.ColumnAdapter,
        quantityAdapter = BigDecimalColumnAdapter,
        lotsAdapter = IntColumnAdapter,
        sideAdapter = TradeExecutionSide.ColumnAdapter,
        priceAdapter = BigDecimalColumnAdapter,
        timestampAdapter = InstantReadableColumnAdapter,
    ),
    TradeStopAdapter = TradeStop.Adapter(
        tradeIdAdapter = TradeIdColumnAdapter,
        priceAdapter = BigDecimalColumnAdapter,
    ),
    TradeTargetAdapter = TradeTarget.Adapter(
        tradeIdAdapter = TradeIdColumnAdapter,
        priceAdapter = BigDecimalColumnAdapter,
    ),
    AttachmentFileAdapter = AttachmentFile.Adapter(
        idAdapter = AttachmentFileIdColumnAdapter,
    ),
    TradeAttachmentAdapter = TradeAttachment.Adapter(
        tradeIdAdapter = TradeIdColumnAdapter,
        fileIdAdapter = AttachmentFileIdColumnAdapter,
    ),
    TradeNoteAdapter = TradeNote.Adapter(
        idAdapter = TradeNoteIdColumnAdapter,
        tradeIdAdapter = TradeIdColumnAdapter,
        addedAdapter = InstantReadableColumnAdapter,
        lastEditedAdapter = InstantReadableColumnAdapter,
    ),
    TradeTagAdapter = TradeTag.Adapter(
        idAdapter = TradeTagIdColumnAdapter,
        colorAdapter = IntColumnAdapter,
    ),
    TradeToExecutionMapAdapter = TradeToExecutionMap.Adapter(
        tradeIdAdapter = TradeIdColumnAdapter,
        executionIdAdapter = TradeExecutionIdColumnAdapter,
        overrideQuantityAdapter = BigDecimalColumnAdapter,
    ),
    TradeToTagMapAdapter = TradeToTagMap.Adapter(
        tradeIdAdapter = TradeIdColumnAdapter,
        tagIdAdapter = TradeTagIdColumnAdapter,
    ),
    ReviewAdapter = Review.Adapter(
        idAdapter = ReviewIdColumnAdapter,
        tradeIdsAdapter = TradeIdListColumnAdapter,
        createdAdapter = InstantReadableColumnAdapter,
    ),
    TradeExcursionsAdapter = TradeExcursions.Adapter(
        tradeIdAdapter = TradeIdColumnAdapter,
        tradeMfePriceAdapter = BigDecimalColumnAdapter,
        tradeMfePnlAdapter = BigDecimalColumnAdapter,
        tradeMaePriceAdapter = BigDecimalColumnAdapter,
        tradeMaePnlAdapter = BigDecimalColumnAdapter,
        sessionMfePriceAdapter = BigDecimalColumnAdapter,
        sessionMfePnlAdapter = BigDecimalColumnAdapter,
        sessionMaePriceAdapter = BigDecimalColumnAdapter,
        sessionMaePnlAdapter = BigDecimalColumnAdapter,
    ),
)
