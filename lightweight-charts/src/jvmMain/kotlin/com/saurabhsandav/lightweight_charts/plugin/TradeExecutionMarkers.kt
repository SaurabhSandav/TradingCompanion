package com.saurabhsandav.lightweight_charts.plugin

import com.saurabhsandav.lightweight_charts.ISeriesPrimitive
import com.saurabhsandav.lightweight_charts.data.Time
import com.saurabhsandav.lightweight_charts.utils.LwcJson
import com.saurabhsandav.lightweight_charts.utils.SerializableColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
class TradeExecutionMarkers(
    val options: Options = Options(),
) : ISeriesPrimitive {

    private var callMember: ((String) -> Unit)? = null

    override fun initializer(callMember: (String) -> Unit): String {

        check(this.callMember == null) { "TradeExecutionMarkers already attached" }

        this.callMember = callMember

        val optionsJson = LwcJson.encodeToString(options)

        return "new TradeExecutionMarkers($optionsJson)"
    }

    fun setExecutions(executions: List<Execution>) {

        val markersJson = LwcJson.encodeToString(executions)
        val callMember = checkNotNull(callMember) { "TradeExecutionMarkers not attached" }

        callMember("executions = $markersJson")
    }

    @Serializable
    data class Options(
        val buyFillColor: SerializableColor? = null,
        val buyTextColor: SerializableColor? = null,
        val sellFillColor: SerializableColor? = null,
        val sellTextColor: SerializableColor? = null,
    )

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
        Sell,
    }
}
