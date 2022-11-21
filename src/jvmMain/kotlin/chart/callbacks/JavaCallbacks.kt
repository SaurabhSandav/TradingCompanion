package chart.callbacks

import chart.ISeriesApi
import chart.data.GeneralData
import chart.data.Time
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

internal class JavaCallbacks(private val seriesList: List<ISeriesApi<*>>) {

    internal val subscribeClickCallbacks = mutableListOf<MouseEventHandler>()
    internal val subscribeCrosshairMoveCallbacks = mutableListOf<MouseEventHandler>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun subscribeClickMouseEventHandler(paramsJson: String) {

        // To prevent exceptions being swallowed by JS
        coroutineScope.launch {

            val params = MouseEventParams(paramsJson, seriesList)

            subscribeClickCallbacks.forEach { it.onEvent(params) }
        }
    }

    fun subscribeCrosshairMoveMouseEventHandler(paramsJson: String) {

        // To prevent exceptions being swallowed by JS
        coroutineScope.launch {

            val params = MouseEventParams(paramsJson, seriesList)

            subscribeCrosshairMoveCallbacks.forEach { it.onEvent(params) }
        }
    }

    private fun MouseEventParams(jsonStr: String, seriesList: List<ISeriesApi<*>>): MouseEventParams {

        val paramsJson = Json.parseToJsonElement(jsonStr)

        val time = when (val time = paramsJson.jsonObject["time"]) {
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

        val point = paramsJson.jsonObject["point"]?.let {
            val x = it.jsonObject["x"]!!.jsonPrimitive.float
            val y = it.jsonObject["y"]!!.jsonPrimitive.float
            Point(x = x, y = y)
        }

        val seriesPrices = paramsJson.jsonObject["seriesPrices"]?.jsonArray?.associate {

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
