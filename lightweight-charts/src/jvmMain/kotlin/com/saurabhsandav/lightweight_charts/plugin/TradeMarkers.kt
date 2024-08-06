package com.saurabhsandav.lightweight_charts.plugin

import com.saurabhsandav.lightweight_charts.ISeriesPrimitive
import com.saurabhsandav.lightweight_charts.data.Time
import com.saurabhsandav.lightweight_charts.utils.LwcJson
import com.saurabhsandav.lightweight_charts.utils.SerializableColor
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
class TradeMarkers(
    val options: Options = Options(),
) : ISeriesPrimitive {

    private var _callMember: ((String) -> Unit)? = null
    private val callMember: ((String) -> Unit)
        get() = checkNotNull(_callMember) { "TradeMarkers not attached" }

    override fun initializer(callMember: (String) -> Unit): String {

        check(_callMember == null) { "TradeMarkers already attached" }

        _callMember = callMember

        val optionsJson = LwcJson.encodeToString(options)

        return "new TradeMarkers($optionsJson)"
    }

    fun setTrades(trades: List<Trade>) {

        val tradesJson = LwcJson.encodeToString(trades)

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
