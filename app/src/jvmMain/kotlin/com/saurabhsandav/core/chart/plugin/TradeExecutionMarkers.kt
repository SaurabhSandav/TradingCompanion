package com.saurabhsandav.core.chart.plugin

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.chart.ISeriesPrimitive
import com.saurabhsandav.core.chart.IsJsonElement
import com.saurabhsandav.core.chart.data.Time
import com.saurabhsandav.core.ui.common.toHexString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TradeExecutionMarkers(
    private val buyFillColor: Color? = null,
    private val buyTextColor: Color? = null,
    private val sellFillColor: Color? = null,
    private val sellTextColor: Color? = null,
) : ISeriesPrimitive {

    private var _callMember: ((String) -> Unit)? = null
    private val callMember: ((String) -> Unit)
        get() = checkNotNull(_callMember) { "TradeExecutionMarkers not attached" }

    override fun initializer(callMember: (String) -> Unit): String {

        check(_callMember == null) { "TradeExecutionMarkers already attached" }

        _callMember = callMember

        val optionsJson = buildJsonObject {
            buyFillColor?.let { put("buyFillColor", it.toHexString()) }
            buyTextColor?.let { put("buyTextColor", it.toHexString()) }
            sellFillColor?.let { put("sellFillColor", it.toHexString()) }
            sellTextColor?.let { put("sellTextColor", it.toHexString()) }
        }

        return "new TradeExecutionMarkers($optionsJson)"
    }

    fun setExecutions(executions: List<Execution>) {

        val markersJson = JsonArray(executions.map { it.toJsonElement() })

        callMember("executions = $markersJson")
    }

    class Execution(
        val time: Time,
        val price: Number,
        val side: TradeExecutionSide,
    ) : IsJsonElement {

        override fun toJsonElement(): JsonElement = buildJsonObject {
            put("time", time.toJsonElement())
            put("price", price)
            put("side", side.strValue)
        }
    }

    enum class TradeExecutionSide(val strValue: String) {
        Buy("buy"),
        Sell("sell");
    }
}
