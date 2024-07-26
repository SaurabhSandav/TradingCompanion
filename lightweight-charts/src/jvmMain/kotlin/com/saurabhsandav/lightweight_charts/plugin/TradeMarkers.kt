package com.saurabhsandav.lightweight_charts.plugin

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.lightweight_charts.ISeriesPrimitive
import com.saurabhsandav.lightweight_charts.IsJsonElement
import com.saurabhsandav.lightweight_charts.data.Time
import com.saurabhsandav.lightweight_charts.toHexString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TradeMarkers(
    private val entryLabelOptions: LabelOptions? = null,
    private val stopFillColor: Color? = null,
    private val stopLabelOptions: LabelOptions? = null,
    private val targetFillColor: Color? = null,
    private val targetLabelOptions: LabelOptions? = null,
    private val exitArrowColor: Color? = null,
    private val separatorColor: Color? = null,
    private val showLabels: Boolean? = null,
) : ISeriesPrimitive {

    private var _callMember: ((String) -> Unit)? = null
    private val callMember: ((String) -> Unit)
        get() = checkNotNull(_callMember) { "TradeMarkers not attached" }

    override fun initializer(callMember: (String) -> Unit): String {

        check(_callMember == null) { "TradeMarkers already attached" }

        _callMember = callMember

        val optionsJson = buildJsonObject {
            entryLabelOptions?.let { put("entryLabelOptions", it.toJsonElement()) }
            stopFillColor?.let { put("stopFillColor", it.toHexString()) }
            stopLabelOptions?.let { put("stopLabelOptions", it.toJsonElement()) }
            targetFillColor?.let { put("targetFillColor", it.toHexString()) }
            targetLabelOptions?.let { put("targetLabelOptions", it.toJsonElement()) }
            exitArrowColor?.let { put("exitArrowColor", it.toHexString()) }
            separatorColor?.let { put("separatorColor", it.toHexString()) }
            showLabels?.let { put("showLabels", it) }
        }

        return "new TradeMarkers($optionsJson)"
    }

    fun setTrades(trades: List<Trade>) {

        val tradesJson = JsonArray(trades.map { it.toJsonElement() })

        callMember("trades = $tradesJson")
    }

    class Trade(
        val entryTime: Time,
        val entryPrice: Number,
        val exitTime: Time,
        val exitPrice: Number,
        val stopPrice: Number,
        val targetPrice: Number,
    ) : IsJsonElement {

        override fun toJsonElement(): JsonElement = buildJsonObject {
            put("entryTime", entryTime.toJsonElement())
            put("entryPrice", entryPrice)
            put("exitTime", exitTime.toJsonElement())
            put("exitPrice", exitPrice)
            put("stopPrice", stopPrice)
            put("targetPrice", targetPrice)
        }
    }

    data class LabelOptions(
        val labelColor: Color,
        val labelTextColor: Color,
    ) : IsJsonElement {

        override fun toJsonElement(): JsonElement = buildJsonObject {
            put("labelColor", labelColor.toHexString())
            put("labelTextColor", labelTextColor.toHexString())
        }
    }
}
