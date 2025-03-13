package com.saurabhsandav.lightweightcharts.plugin

import com.saurabhsandav.lightweightcharts.ISeriesPrimitive
import com.saurabhsandav.lightweightcharts.data.Time
import com.saurabhsandav.lightweightcharts.utils.LwcJson
import com.saurabhsandav.lightweightcharts.utils.SerializableColor
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
