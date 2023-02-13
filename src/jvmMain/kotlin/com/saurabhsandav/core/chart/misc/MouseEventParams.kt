package com.saurabhsandav.core.chart.misc

import com.saurabhsandav.core.chart.ISeriesApi
import com.saurabhsandav.core.chart.callbacks.timeFromCallbackJson
import com.saurabhsandav.core.chart.data.CandlestickData
import com.saurabhsandav.core.chart.data.GeneralData
import com.saurabhsandav.core.chart.data.Time
import kotlinx.serialization.json.*

data class MouseEventParams(
    val time: Time?,
    val point: Point?,
    val seriesPrices: Map<ISeriesApi<*>, GeneralData>,
) {

    fun getSeriesPrices(key: ISeriesApi<CandlestickData>): GeneralData.BarPrices? {
        return seriesPrices[key] as GeneralData.BarPrices?
    }

    fun getSeriesPrice(key: ISeriesApi<*>): GeneralData.BarPrice? {
        return seriesPrices[key] as GeneralData.BarPrice?
    }

    companion object {

        fun fromJson(
            jsonStr: String,
            seriesList: List<ISeriesApi<*>>,
        ): MouseEventParams {

            val paramsElement = Json.parseToJsonElement(jsonStr)

            val time = timeFromCallbackJson(paramsElement.jsonObject["time"])

            val point = paramsElement.jsonObject["point"]?.let {
                val x = it.jsonObject["x"]!!.jsonPrimitive.float
                val y = it.jsonObject["y"]!!.jsonPrimitive.float
                Point(x = x, y = y)
            }

            val seriesPrices = paramsElement.jsonObject["seriesPrices"]?.jsonArray?.mapNotNull {

                val name = it.jsonArray[0].jsonPrimitive.content

                // If series is not found, chart data was probably replaced. In such a case, a race condition occurs
                // where this function may be called with the new series list while callback params were received
                // for the old data. It's a rare occurrence. The best way to deal with it is to pass empty prices.
                val series = seriesList.find { series -> series.name == name } ?: return@mapNotNull null

                val value = when (val data = it.jsonArray[1]) {
                    is JsonObject -> GeneralData.BarPrices(
                        open = data["open"]!!.jsonPrimitive.content.toDouble(),
                        high = data["high"]!!.jsonPrimitive.content.toDouble(),
                        low = data["low"]!!.jsonPrimitive.content.toDouble(),
                        close = data["close"]!!.jsonPrimitive.content.toDouble(),
                    )

                    is JsonPrimitive -> GeneralData.BarPrice(data.content.toDouble())
                    else -> error("MouseEventParams: Invalid GenericData")
                }

                series to value
            }?.toMap().orEmpty()

            return MouseEventParams(
                time = time,
                point = point,
                seriesPrices = seriesPrices,
            )
        }
    }
}

data class Point(
    val x: Float,
    val y: Float,
)
