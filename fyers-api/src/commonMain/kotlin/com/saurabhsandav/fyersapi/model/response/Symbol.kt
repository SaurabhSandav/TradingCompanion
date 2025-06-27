@file:UseSerializers(BigDecimalSerializer::class)

package com.saurabhsandav.fyersapi.model.response

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

@Serializable
public data class Symbol(
    val fyToken: String,
    val isin: String,
    val exSymbol: String,
    val symDetails: String,
    val symTicker: String,
    val exchange: Exchange,
    val segment: Segment,
    val exSymName: String,
    val exToken: Int,
    val exSeries: String,
    val optType: String,
    val underSym: String,
    val underFyTok: String,
    val exInstType: ExchangeInstrumentType,
    val minLotSize: Int,
    val tickSize: BigDecimal,
    val tradingSession: String,
    val lastUpdate: String,
    val expiryDate: String,
    val strikePrice: BigDecimal,
    val qtyFreeze: String,
    val tradeStatus: Int,
    val currencyCode: String,
    val upperPrice: BigDecimal,
    val lowerPrice: BigDecimal,
    val faceValue: BigDecimal,
    val qtyMultiplier: BigDecimal,
    val previousClose: BigDecimal,
    val previousOi: BigDecimal,
    val asmGsmVal: String,
    val exchangeName: String,
    val symbolDesc: String,
    val originalExpDate: String?,
    val is_mtf_tradable: Int,
    val mtf_margin: BigDecimal,
    val stream: String,
)

@Serializable(with = ExchangeSerializer::class)
public enum class Exchange(
    internal val value: Int,
) {
    NSE(10),
    MCX(11),
    BSE(12),
    ;

    internal companion object {

        fun fromInt(intValue: Int): Exchange {
            return enumValues<Exchange>()
                .find { it.value == intValue }
                ?: error("Invalid exchange type: $intValue")
        }
    }
}

private object ExchangeSerializer : KSerializer<Exchange> {

    override val descriptor = PrimitiveSerialDescriptor("ExchangeSerializer", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Exchange {
        return Exchange.fromInt(decoder.decodeInt())
    }

    override fun serialize(
        encoder: Encoder,
        value: Exchange,
    ) {
        encoder.encodeInt(value.value)
    }
}

@Serializable(with = SegmentSerializer::class)
public enum class Segment(
    internal val value: Int,
) {
    CapitalMarket(10),
    EquityDerivatives(11),
    CurrencyDerivatives(12),
    CommodityDerivatives(20),
    ;

    internal companion object {

        fun fromInt(intValue: Int): Segment {
            return enumValues<Segment>()
                .find { it.value == intValue }
                ?: error("Invalid segment type: $intValue")
        }
    }
}

private object SegmentSerializer : KSerializer<Segment> {

    override val descriptor = PrimitiveSerialDescriptor("SegmentSerializer", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Segment {
        return Segment.fromInt(decoder.decodeInt())
    }

    override fun serialize(
        encoder: Encoder,
        value: Segment,
    ) {
        encoder.encodeInt(value.value)
    }
}

@Serializable(with = ExchangeInstrumentTypeSerializer::class)
public enum class ExchangeInstrumentType(
    internal val value: Int,
) {

    // CM segment
    EQ(0),
    PREFSHARES(1),
    DEBENTURES(2),
    WARRANTS(3),
    MISC_NSE_BSE(4),
    SGB(5),
    GSECS(6),
    TBILLS(7),
    MF(8),
    ETF(9),
    INDEX(10),
    MISC_BSE(50),

    // FO, COM segment
    FUTIDX(11),

    // FO segment
    FUTIVX(12),
    FUTSTK(13),
    OPTIDX(14),
    OPTSTK(15),

    // CD segment
    FUTCUR(16),
    FUTIRT(17),
    FUTIRC(18),
    OPTCUR(19),
    UNDCUR(20),
    UNDIRC(21),
    UNDIRT(22),
    UNDIRD(23),
    INDEX_CD(24),
    FUTIRD(25),

    // COM segment
    FUTCOM(30),
    OPTFUT(31),
    OPTCOM(32),
    FUTBAS(33),
    FUTBLN(34),
    FUTENR(35),
    OPTBLN(36),
    OPTFUT_NCOM(37),
    ;

    internal companion object {

        fun fromInt(intValue: Int): ExchangeInstrumentType {
            return enumValues<ExchangeInstrumentType>()
                .find { it.value == intValue }
                ?: error("Invalid exchange instrument type: $intValue")
        }
    }
}

private object ExchangeInstrumentTypeSerializer : KSerializer<ExchangeInstrumentType> {

    override val descriptor = PrimitiveSerialDescriptor("ExchangeInstrumentTypeSerializer", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): ExchangeInstrumentType {
        return ExchangeInstrumentType.fromInt(decoder.decodeInt())
    }

    override fun serialize(
        encoder: Encoder,
        value: ExchangeInstrumentType,
    ) {
        encoder.encodeInt(value.value)
    }
}

private object BigDecimalSerializer : KSerializer<BigDecimal> {

    override val descriptor = PrimitiveSerialDescriptor("BigDecimalSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        return decoder.decodeString().toBigDecimal()
    }

    override fun serialize(
        encoder: Encoder,
        value: BigDecimal,
    ) {
        encoder.encodeString(value.toPlainString())
    }
}
