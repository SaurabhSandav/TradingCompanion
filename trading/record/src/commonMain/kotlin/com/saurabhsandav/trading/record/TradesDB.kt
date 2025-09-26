package com.saurabhsandav.trading.record

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.model.AttachmentFileId
import com.saurabhsandav.trading.record.model.ReviewId
import com.saurabhsandav.trading.record.model.SizingTradeId
import com.saurabhsandav.trading.record.model.TradeExecutionId
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.model.TradeIdListColumnAdapter
import com.saurabhsandav.trading.record.model.TradeNoteId
import com.saurabhsandav.trading.record.model.TradeSide
import com.saurabhsandav.trading.record.model.TradeTagId
import com.saurabhsandav.trading.record.utils.InstantReadableColumnAdapter
import com.saurabhsandav.trading.record.utils.KBigDecimalColumnAdapter

internal fun TradesDB(driver: SqlDriver): TradesDB = TradesDB(
    driver = driver,
    SizingTradeAdapter = SizingTrade.Adapter(
        idAdapter = SizingTradeId.ColumnAdapter,
        entryAdapter = KBigDecimalColumnAdapter,
        stopAdapter = KBigDecimalColumnAdapter,
        symbolIdAdapter = SymbolIdColumnAdapter,
        brokerIdAdapter = BrokerId.ColumnAdapter,
    ),
    TradeAdapter = Trade.Adapter(
        idAdapter = TradeId.ColumnAdapter,
        instrumentAdapter = InstrumentColumnAdapter,
        quantityAdapter = KBigDecimalColumnAdapter,
        closedQuantityAdapter = KBigDecimalColumnAdapter,
        lotsAdapter = IntColumnAdapter,
        sideAdapter = TradeSide.ColumnAdapter,
        averageEntryAdapter = KBigDecimalColumnAdapter,
        entryTimestampAdapter = InstantReadableColumnAdapter,
        averageExitAdapter = KBigDecimalColumnAdapter,
        exitTimestampAdapter = InstantReadableColumnAdapter,
        pnlAdapter = KBigDecimalColumnAdapter,
        feesAdapter = KBigDecimalColumnAdapter,
        netPnlAdapter = KBigDecimalColumnAdapter,
        brokerIdAdapter = BrokerId.ColumnAdapter,
        symbolIdAdapter = SymbolIdColumnAdapter,
    ),
    TradeExecutionAdapter = TradeExecution.Adapter(
        idAdapter = TradeExecutionId.ColumnAdapter,
        instrumentAdapter = InstrumentColumnAdapter,
        quantityAdapter = KBigDecimalColumnAdapter,
        lotsAdapter = IntColumnAdapter,
        sideAdapter = TradeExecutionSide.ColumnAdapter,
        priceAdapter = KBigDecimalColumnAdapter,
        timestampAdapter = InstantReadableColumnAdapter,
        brokerIdAdapter = BrokerId.ColumnAdapter,
        symbolIdAdapter = SymbolIdColumnAdapter,
    ),
    TradeStopAdapter = TradeStop.Adapter(
        tradeIdAdapter = TradeId.ColumnAdapter,
        priceAdapter = KBigDecimalColumnAdapter,
    ),
    TradeTargetAdapter = TradeTarget.Adapter(
        tradeIdAdapter = TradeId.ColumnAdapter,
        priceAdapter = KBigDecimalColumnAdapter,
    ),
    AttachmentFileAdapter = AttachmentFile.Adapter(
        idAdapter = AttachmentFileId.ColumnAdapter,
    ),
    TradeAttachmentAdapter = TradeAttachment.Adapter(
        tradeIdAdapter = TradeId.ColumnAdapter,
        fileIdAdapter = AttachmentFileId.ColumnAdapter,
    ),
    TradeNoteAdapter = TradeNote.Adapter(
        idAdapter = TradeNoteId.ColumnAdapter,
        tradeIdAdapter = TradeId.ColumnAdapter,
        addedAdapter = InstantReadableColumnAdapter,
        lastEditedAdapter = InstantReadableColumnAdapter,
    ),
    TradeTagAdapter = TradeTag.Adapter(
        idAdapter = TradeTagId.ColumnAdapter,
        colorAdapter = IntColumnAdapter,
    ),
    TradeToExecutionMapAdapter = TradeToExecutionMap.Adapter(
        tradeIdAdapter = TradeId.ColumnAdapter,
        executionIdAdapter = TradeExecutionId.ColumnAdapter,
        overrideQuantityAdapter = KBigDecimalColumnAdapter,
    ),
    TradeToTagMapAdapter = TradeToTagMap.Adapter(
        tradeIdAdapter = TradeId.ColumnAdapter,
        tagIdAdapter = TradeTagId.ColumnAdapter,
    ),
    ReviewAdapter = Review.Adapter(
        idAdapter = ReviewId.ColumnAdapter,
        tradeIdsAdapter = TradeIdListColumnAdapter,
        createdAdapter = InstantReadableColumnAdapter,
    ),
    TradeExcursionsAdapter = TradeExcursions.Adapter(
        tradeIdAdapter = TradeId.ColumnAdapter,
        tradeMfePriceAdapter = KBigDecimalColumnAdapter,
        tradeMfePnlAdapter = KBigDecimalColumnAdapter,
        tradeMaePriceAdapter = KBigDecimalColumnAdapter,
        tradeMaePnlAdapter = KBigDecimalColumnAdapter,
        sessionMfePriceAdapter = KBigDecimalColumnAdapter,
        sessionMfePnlAdapter = KBigDecimalColumnAdapter,
        sessionMaePriceAdapter = KBigDecimalColumnAdapter,
        sessionMaePnlAdapter = KBigDecimalColumnAdapter,
    ),
    BrokerAdapter = Broker.Adapter(
        idAdapter = BrokerId.ColumnAdapter,
    ),
    SymbolAdapter = Symbol.Adapter(
        idAdapter = SymbolIdColumnAdapter,
        brokerIdAdapter = BrokerId.ColumnAdapter,
        instrumentAdapter = InstrumentColumnAdapter,
    ),
)

object SymbolIdColumnAdapter : ColumnAdapter<SymbolId, String> {

    override fun decode(databaseValue: String): SymbolId = SymbolId(databaseValue)

    override fun encode(value: SymbolId): String = value.value
}

object InstrumentColumnAdapter : ColumnAdapter<Instrument, String> {
    override fun decode(databaseValue: String): Instrument = Instrument.fromString(databaseValue)

    override fun encode(value: Instrument): String = value.strValue
}
