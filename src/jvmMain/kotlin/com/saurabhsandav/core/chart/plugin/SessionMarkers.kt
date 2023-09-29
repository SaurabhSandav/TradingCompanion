package com.saurabhsandav.core.chart.plugin

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.chart.ISeriesPrimitive
import com.saurabhsandav.core.chart.data.Time
import com.saurabhsandav.core.ui.common.toHexString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SessionMarkers(
    private val lineColor: Color? = null,
) : ISeriesPrimitive {

    private var _callMember: ((String) -> Unit)? = null
    private val callMember: ((String) -> Unit)
        get() = checkNotNull(_callMember) { "SessionMarkers not attached" }

    override fun initializer(callMember: (String) -> Unit): String {

        check(_callMember == null) { "SessionMarkers already attached" }

        _callMember = callMember

        val optionsJson = buildJsonObject {
            lineColor?.let { put("lineColor", it.toHexString()) }
        }

        return "new SessionMarkers($optionsJson)"
    }

    fun setTimes(times: List<Time>) {

        val timesJson = JsonArray(times.map { it.toJsonElement() })

        callMember("times = $timesJson")
    }
}
