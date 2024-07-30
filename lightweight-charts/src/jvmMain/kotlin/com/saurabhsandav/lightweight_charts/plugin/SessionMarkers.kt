package com.saurabhsandav.lightweight_charts.plugin

import com.saurabhsandav.lightweight_charts.ISeriesPrimitive
import com.saurabhsandav.lightweight_charts.data.Time
import kotlinx.css.Color
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
            lineColor?.let { put("lineColor", it.value) }
        }

        return "new SessionMarkers($optionsJson)"
    }

    fun setTimes(times: List<Time>) {

        val timesJson = JsonArray(times.map { it.toJsonElement() })

        callMember("times = $timesJson")
    }
}
