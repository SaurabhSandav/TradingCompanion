package com.saurabhsandav.lightweight_charts.data

import com.saurabhsandav.lightweight_charts.ISeriesApi
import com.saurabhsandav.lightweight_charts.callbacks.timeFromCallbackJson
import kotlinx.serialization.json.*

data class MouseEventParams(
    val time: Time?,
    val logical: Float?,
    val point: Point?,
    val seriesData: Map<ISeriesApi<*>, SeriesData>,
) {

    companion object {

        fun fromJson(
            jsonStr: String,
            seriesList: List<ISeriesApi<*>>,
        ): MouseEventParams {

            val paramsElement = Json.parseToJsonElement(jsonStr)

            val time = timeFromCallbackJson(paramsElement.jsonObject["time"])

            val logical = paramsElement.jsonObject["logical"]?.jsonPrimitive?.float

            val point = paramsElement.jsonObject["point"]?.let {
                val x = it.jsonObject["x"]!!.jsonPrimitive.float
                val y = it.jsonObject["y"]!!.jsonPrimitive.float
                Point(x = x, y = y)
            }

            val seriesData = paramsElement.jsonObject["seriesData"]?.jsonArray?.mapNotNull {

                val name = it.jsonArray[0].jsonPrimitive.content

                // If series is not found, chart data was probably replaced. In such a case, a race condition occurs
                // where this function may be called with the new series list while callback params were received
                // for the old data. It's a rare occurrence. The best way to deal with it is to pass empty prices.
                val series = seriesList.find { series -> series.name == name } ?: return@mapNotNull null

                val data = it.jsonArray[1].jsonObject

                val value = when {
                    data.containsKey("open") -> CandlestickData.Item(
                        time = timeFromCallbackJson(data["time"])!!,
                        open = data["open"]!!.jsonPrimitive.double,
                        high = data["high"]!!.jsonPrimitive.double,
                        low = data["low"]!!.jsonPrimitive.double,
                        close = data["close"]!!.jsonPrimitive.double,
                    )

                    data.containsKey("value") -> SingleValueData(
                        time = timeFromCallbackJson(data["time"])!!,
                        value = data["value"]!!.jsonPrimitive.double,
                    )

                    else -> error("MouseEventParams: Invalid Data")
                }

                series to value
            }?.toMap().orEmpty()

            return MouseEventParams(
                time = time,
                logical = logical,
                point = point,
                seriesData = seriesData,
            )
        }
    }
}

data class Point(
    val x: Float,
    val y: Float,
)
