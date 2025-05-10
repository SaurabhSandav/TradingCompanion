package com.saurabhsandav.fyersapi.model.response

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

@Serializable
public data class Symbol(
    val fyToken: String,
    val details: String,
    val exchangeInstrumentType: ExchangeInstrumentType,
    val minLotSize: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val tickSize: BigDecimal,
    val isin: String,
    val tradingSession: String,
    val lastUpdateDate: String,
    val expiryDate: String,
    val ticker: String,
    val scripCode: Int,
    val underlyingScripCode: String,
    @Serializable(with = BigDecimalSerializer::class)
    val strikePrice: BigDecimal,
    val optionType: String,
    val underlyingFyToken: String,
)

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

// TODO: Move to shared module
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
