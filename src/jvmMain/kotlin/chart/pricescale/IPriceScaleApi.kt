package chart.pricescale

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class IPriceScaleApi(
    private val receiver: String,
    private val executeJs: (String) -> Unit,
    private val json: Json,
) {

    fun applyOptions(options: PriceScaleOptions) {

        val optionsStr = json.encodeToString(options.toJsonObject())

        executeJs("$receiver.priceScale().applyOptions($optionsStr);")
    }
}

data class PriceScaleOptions(
    val scaleMargins: PriceScaleMargins? = null,
) {

    fun toJsonObject(): JsonObject = buildJsonObject {
        scaleMargins?.let { put("scaleMargins", it.toJsonObject()) }
    }
}

data class PriceScaleMargins(
    val top: Number? = null,
    val bottom: Number? = null,
) {

    fun toJsonObject(): JsonObject = buildJsonObject {
        top?.let { put("top", it) }
        bottom?.let { put("bottom", it) }
    }
}
