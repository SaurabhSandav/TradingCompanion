package chart.timescale

internal class TimeScaleOptions(
    private val timeVisible: Boolean? = null,
) {

    fun toJson(): String = buildString {

        append("{")

        timeVisible?.let { append("timeVisible: ${it},") }

        append("}")
    }
}
