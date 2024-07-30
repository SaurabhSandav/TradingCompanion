package com.saurabhsandav.lightweight_charts.plugin

import com.saurabhsandav.lightweight_charts.ISeriesPrimitive
import com.saurabhsandav.lightweight_charts.IsJsonElement
import com.saurabhsandav.lightweight_charts.data.Time
import kotlinx.css.Color
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
            buyFillColor?.let { put("buyFillColor", it.value) }
            buyTextColor?.let { put("buyTextColor", it.value) }
            sellFillColor?.let { put("sellFillColor", it.value) }
            sellTextColor?.let { put("sellTextColor", it.value) }
        }

        return "new TradeExecutionMarkers($optionsJson)"
    }

    fun setExecutions(executions: List<Execution>) {

        val markersJson = JsonArray(executions.map { it.toJsonElement() })

        callMember("executions = $markersJson")
    }

    class Execution(
        val time: Time,
        val price: Double,
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
