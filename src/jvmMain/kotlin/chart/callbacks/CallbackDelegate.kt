package chart.callbacks

import chart.ISeriesApi
import chart.data.GeneralData
import chart.data.Time
import kotlinx.serialization.json.*

internal class CallbackDelegate(
    private val chartName: String,
    private val seriesList: List<ISeriesApi<*>>,
) {

    internal val subscribeClickCallbacks = mutableListOf<MouseEventHandler>()
    internal val subscribeCrosshairMoveCallbacks = mutableListOf<MouseEventHandler>()

    fun onCallback(callbackStr: String) {

        val callbackElement = Json.parseToJsonElement(callbackStr)

        val chartCallback = ChartCallback(
            chartName = callbackElement.jsonObject["chartName"]!!.jsonPrimitive.content,
            callbackType = callbackElement.jsonObject["callbackType"]!!.jsonPrimitive.content,
            message = callbackElement.jsonObject["message"]!!.toString(),
        )

        // Callback not related to this chart
        if (chartCallback.chartName != chartName) return

        when (chartCallback.callbackType) {
            "subscribeClickCallback" -> {

                val params = MouseEventParams(Json.parseToJsonElement(chartCallback.message), seriesList)

                subscribeClickCallbacks.forEach { it.onEvent(params) }
            }

            "subscribeCrosshairMoveCallback" -> {

                val params = MouseEventParams(Json.parseToJsonElement(chartCallback.message), seriesList)

                subscribeCrosshairMoveCallbacks.forEach { it.onEvent(params) }
            }
        }
    }

    private fun MouseEventParams(
        paramsElement: JsonElement,
        seriesList: List<ISeriesApi<*>>,
    ): MouseEventParams {

        val time = when (val time = paramsElement.jsonObject["time"]) {
            is JsonObject -> Time.BusinessDay(
                year = time["year"]!!.jsonPrimitive.int,
                month = time["month"]!!.jsonPrimitive.int,
                day = time["day"]!!.jsonPrimitive.int,
            )

            is JsonPrimitive -> when {
                time.isString -> Time.String(time.content)
                else -> Time.UTCTimestamp(time.long)
            }

            else -> null
        }

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
