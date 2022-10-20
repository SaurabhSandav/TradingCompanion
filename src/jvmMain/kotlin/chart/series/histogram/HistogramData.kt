package chart.series.histogram

import androidx.compose.ui.graphics.Color
import chart.series.data.SingleValueData
import chart.series.data.Time
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ui.common.toHexString

class HistogramData(
    time: Time,
    value: Number,
    val color: Color? = null,
) : SingleValueData(time, value) {

    override fun toJsonObject(): JsonObject = buildJsonObject {

        putSingleValueDataElements(this@HistogramData)

        color?.let { put("color", it.toHexString()) }
    }
}
