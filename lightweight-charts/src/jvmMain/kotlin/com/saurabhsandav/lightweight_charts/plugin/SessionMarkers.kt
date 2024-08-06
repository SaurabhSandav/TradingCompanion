package com.saurabhsandav.lightweight_charts.plugin

import com.saurabhsandav.lightweight_charts.ISeriesPrimitive
import com.saurabhsandav.lightweight_charts.data.Time
import com.saurabhsandav.lightweight_charts.utils.LwcJson
import com.saurabhsandav.lightweight_charts.utils.SerializableColor
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
class SessionMarkers(
    val options: Options = Options(),
) : ISeriesPrimitive {

    private var _callMember: ((String) -> Unit)? = null
    private val callMember: ((String) -> Unit)
        get() = checkNotNull(_callMember) { "SessionMarkers not attached" }

    override fun initializer(callMember: (String) -> Unit): String {

        check(_callMember == null) { "SessionMarkers already attached" }

        _callMember = callMember

        val optionsJson = LwcJson.encodeToString(options)

        return "new SessionMarkers($optionsJson)"
    }

    fun setTimes(times: List<Time>) {

        val timesJson = LwcJson.encodeToString(times)

        callMember("times = $timesJson")
    }

    @Serializable
    data class Options(
        val lineColor: SerializableColor? = null,
    )
}
