package com.saurabhsandav.lightweight_charts.plugin

import com.saurabhsandav.lightweight_charts.ISeriesPrimitive
import com.saurabhsandav.lightweight_charts.data.Time
import com.saurabhsandav.lightweight_charts.utils.LwcJson
import com.saurabhsandav.lightweight_charts.utils.SerializableColor
import kotlinx.serialization.Serializable

@Serializable
class TradeMarkers(
    val options: Options = Options(),
) : ISeriesPrimitive {

    private var callMember: ((String) -> Unit)? = null

    override fun initializer(callMember: (String) -> Unit): String {

        check(this.callMember == null) { "TradeMarkers already attached" }

        this.callMember = callMember

        val optionsJson = LwcJson.encodeToString(options)

        return "new TradeMarkers($optionsJson)"
    }

    fun setTrades(trades: List<Trade>) {

        val tradesJson = LwcJson.encodeToString(trades)
        val callMember = checkNotNull(callMember) { "TradeMarkers not attached" }

        callMember("trades = $tradesJson")
    }

    @Serializable
    data class Options(
        val entryLabelOptions: LabelOptions? = null,
        val stopFillColor: SerializableColor? = null,
        val stopLabelOptions: LabelOptions? = null,
        val targetFillColor: SerializableColor? = null,
        val targetLabelOptions: LabelOptions? = null,
        val exitArrowColor: SerializableColor? = null,
        val separatorColor: SerializableColor? = null,
        val showLabels: Boolean? = null,
    )

    @Serializable
    data class Trade(
        val entryTime: Time,
        val entryPrice: Double,
        val exitTime: Time,
        val exitPrice: Double,
        val stopPrice: Double,
        val targetPrice: Double,
    )

    @Serializable
    data class LabelOptions(
        val labelColor: SerializableColor,
        val labelTextColor: SerializableColor,
    )
}
