package com.saurabhsandav.lightweight_charts.options

import com.saurabhsandav.lightweight_charts.PriceScaleOptions
import com.saurabhsandav.lightweight_charts.options.common.Background
import com.saurabhsandav.lightweight_charts.options.common.LineStyle
import com.saurabhsandav.lightweight_charts.options.common.LineWidth
import com.saurabhsandav.lightweight_charts.utils.SerializableColor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class ChartOptions(
    val width: Double? = null,
    val height: Double? = null,
    val autoSize: Boolean? = null,
    val layout: LayoutOptions? = null,
    val leftPriceScale: PriceScaleOptions? = null,
    val rightPriceScale: PriceScaleOptions? = null,
    val timeScale: TimeScaleOptions? = null,
    val crosshair: CrosshairOptions? = null,
    val grid: GridOptions? = null,
) {

    @Serializable
    data class LayoutOptions(
        val background: Background? = null,
        val textColor: SerializableColor? = null,
    )

    @Serializable
    data class CrosshairOptions(
        val mode: CrosshairMode? = null,
        val vertLine: CrosshairLineOptions? = null,
        val horzLine: CrosshairLineOptions? = null,
    ) {

        @Serializable(with = CrosshairMode.Serializer::class)
        enum class CrosshairMode(private val intValue: Int) {

            Normal(0),
            Magnet(1),
            Hidden(2);

            internal object Serializer : KSerializer<CrosshairMode> {

                override val descriptor = PrimitiveSerialDescriptor("CrosshairModeSerializer", PrimitiveKind.INT)

                override fun deserialize(decoder: Decoder): CrosshairMode {
                    val intValue = decoder.decodeInt()
                    return CrosshairMode.entries.find { it.intValue == intValue } ?: error("Invalid CrosshairMode")
                }

                override fun serialize(encoder: Encoder, value: CrosshairMode) {
                    encoder.encodeInt(value.intValue)
                }
            }
        }

        @Serializable
        data class CrosshairLineOptions(
            val color: SerializableColor? = null,
            val width: LineWidth? = null,
            val style: LineStyle? = null,
            val visible: Boolean? = null,
            val labelVisible: Boolean? = null,
            val labelBackgroundColor: SerializableColor? = null,
        )
    }

    @Serializable
    data class GridOptions(
        val vertLines: GridLineOptions? = null,
        val horzLines: GridLineOptions? = null,
    ) {

        @Serializable
        data class GridLineOptions(
            val color: SerializableColor? = null,
            val style: LineStyle? = null,
            val visible: Boolean? = null,
        )
    }
}
