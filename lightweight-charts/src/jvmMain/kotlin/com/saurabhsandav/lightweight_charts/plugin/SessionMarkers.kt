package com.saurabhsandav.lightweight_charts.plugin

import com.saurabhsandav.lightweight_charts.ISeriesPrimitive
import com.saurabhsandav.lightweight_charts.data.Time
import com.saurabhsandav.lightweight_charts.utils.LwcJson
import com.saurabhsandav.lightweight_charts.utils.SerializableColor
import kotlinx.serialization.Serializable

@Serializable
class SessionMarkers(
    val options: Options = Options(),
) : ISeriesPrimitive {

    private var callMember: ((String) -> Unit)? = null

    override fun initializer(callMember: (String) -> Unit): String {

        check(this.callMember == null) { "SessionMarkers already attached" }

        this.callMember = callMember

        val optionsJson = LwcJson.encodeToString(options)

        return "new SessionMarkers($optionsJson)"
    }

    fun setTimes(times: List<Time>) {

        val timesJson = LwcJson.encodeToString(times)
        val callMember = checkNotNull(callMember) { "SessionMarkers not attached" }

        callMember("times = $timesJson")
    }

    @Serializable
    data class Options(
        val lineColor: SerializableColor? = null,
    )
}
