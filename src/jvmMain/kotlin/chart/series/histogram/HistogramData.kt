package chart.series.histogram

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import chart.series.SingleValueData
import chart.series.Time
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class HistogramData(
    time: Time,
    value: Number,
    val color: Color? = null,
) : SingleValueData(time, value) {

    override fun toJsonObject(): JsonObject = buildJsonObject {

        putSingleValueDataElements(this@HistogramData)

        fun Int.hexToString() = String.format("#%06X", 0xFFFFFF and this)

        color?.let { put("color", it.toArgb().hexToString()) }
    }
}
