package com.saurabhsandav.lightweight_charts.plugin

import com.saurabhsandav.lightweight_charts.ISeriesPrimitive
import com.saurabhsandav.lightweight_charts.data.Time
import com.saurabhsandav.lightweight_charts.utils.LwcJson
import kotlinx.css.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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

        val markersJson = LwcJson.encodeToString(executions)

        callMember("executions = $markersJson")
    }

    @Serializable
    data class Execution(
        val time: Time,
        val price: Double,
        val side: ExecutionSide,
    )

    @Serializable
    enum class ExecutionSide {

        @SerialName("buy")
        Buy,

        @SerialName("sell")
        Sell;
    }
}
