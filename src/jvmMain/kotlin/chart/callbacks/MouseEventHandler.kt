package chart.callbacks

import chart.ISeriesApi
import chart.data.CandlestickData
import chart.data.GeneralData
import chart.data.Time
import kotlinx.serialization.json.*

fun interface MouseEventHandler {

    fun onEvent(params: MouseEventParams)
}

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

            val seriesPrices = paramsElement.jsonObject["seriesPrices"]?.jsonArray?.associate {

                val name = it.jsonArray[0].jsonPrimitive.content
                val series = seriesList.find { series -> series.name == name }!!

                val value = when (val data = it.jsonArray[1]) {
                    is JsonObject -> GeneralData.BarPrices(
                        open = data["open"]!!.jsonPrimitive.content.toBigDecimal(),
                        high = data["high"]!!.jsonPrimitive.content.toBigDecimal(),
                        low = data["low"]!!.jsonPrimitive.content.toBigDecimal(),
                        close = data["close"]!!.jsonPrimitive.content.toBigDecimal(),
                    )

                    is JsonPrimitive -> GeneralData.BarPrice(data.content.toBigDecimal())
                    else -> error("MouseEventParams: Invalid GenericData")
                }

                series to value
            }.orEmpty()

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
