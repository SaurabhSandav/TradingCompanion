package com.saurabhsandav.trading.record.export

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.model.AttachmentFileId
import com.saurabhsandav.trading.record.model.ReviewId
import com.saurabhsandav.trading.record.model.SizingTradeId
import com.saurabhsandav.trading.record.model.TradeExecutionId
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.model.TradeNoteId
import com.saurabhsandav.trading.record.model.TradeSide
import com.saurabhsandav.trading.record.model.TradeTagId
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ExportTradeExecution(
    val id: TradeExecutionId,
    val brokerId: BrokerId,
    val instrument: Instrument,
    val symbolId: SymbolId,
    val quantity: KBigDecimal,
    val lots: Int?,
    val side: TradeExecutionSide,
    val price: KBigDecimal,
    val timestamp: Instant,
    val locked: Boolean,
)

@Serializable
data class ExportTrade(
    val id: TradeId,
    val brokerId: BrokerId,
    val symbolId: SymbolId,
    val instrument: Instrument,
    val quantity: KBigDecimal,
    val closedQuantity: KBigDecimal,
    val lots: Int?,
    val side: TradeSide,
    val averageEntry: KBigDecimal,
    val entryTimestamp: Instant,
    val averageExit: KBigDecimal?,
    val exitTimestamp: Instant?,
    val pnl: KBigDecimal,
    val fees: KBigDecimal,
    val netPnl: KBigDecimal,
    val isClosed: Boolean,
)

@Serializable
data class ExportTradeStop(
    val tradeId: TradeId,
    val price: KBigDecimal,
    val isPrimary: Boolean,
)

@Serializable
data class ExportTradeTarget(
    val tradeId: TradeId,
    val price: KBigDecimal,
    val isPrimary: Boolean,
)

@Serializable
data class ExportTradeNote(
    val id: TradeNoteId,
    val tradeId: TradeId,
    val note: String,
    val added: Instant,
    val lastEdited: Instant?,
)

@Serializable
data class ExportTradeAttachment(
    val tradeId: TradeId,
    val fileId: AttachmentFileId,
    val name: String,
    val description: String,
)

@Serializable
data class ExportTradeExcursions(
    val tradeId: TradeId,
    val tradeMfePrice: KBigDecimal,
    val tradeMfePnl: KBigDecimal,
    val tradeMaePrice: KBigDecimal,
    val tradeMaePnl: KBigDecimal,
    val sessionMfePrice: KBigDecimal,
    val sessionMfePnl: KBigDecimal,
    val sessionMaePrice: KBigDecimal,
    val sessionMaePnl: KBigDecimal,
)

@Serializable
data class ExportTradeTag(
    val id: TradeTagId,
    val name: String,
    val description: String,
    val color: Int?,
)

@Serializable
data class ExportReview(
    val id: ReviewId,
    val title: String,
    val tradeIds: List<TradeId>,
    val review: String,
    val created: Instant,
    val isPinned: Boolean,
)

@Serializable
data class ExportAttachmentFile(
    val id: AttachmentFileId,
    val fileName: String,
    val checksum: String,
    val mimeType: String?,
)

@Serializable
data class ExportBroker(
    val id: BrokerId,
    val name: String,
)

@Serializable
data class ExportSymbol(
    val id: SymbolId,
    val brokerId: BrokerId,
    val instrument: Instrument,
    val exchange: String,
    val ticker: String,
    val description: String?,
)

@Serializable
data class ExportSizingTrade(
    val id: SizingTradeId,
    val brokerId: BrokerId,
    val symbolId: SymbolId,
    val entry: KBigDecimal,
    val stop: KBigDecimal,
)

@Serializable
data class ExportTradeToExecutionMap(
    val tradeId: TradeId,
    val executionId: TradeExecutionId,
    val overrideQuantity: KBigDecimal?,
)

@Serializable
data class ExportTradeToTagMap(
    val tradeId: TradeId,
    val tagId: TradeTagId,
)
