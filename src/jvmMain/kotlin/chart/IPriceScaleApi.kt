package chart

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class IPriceScaleApi(
    private val receiver: String,
    private val executeJs: (String) -> Unit,
) {

    fun applyOptions(options: PriceScaleOptions) {

        val optionsJson = options.toJsonElement()

        executeJs("$receiver.priceScale().applyOptions($optionsJson);")
    }
}

data class PriceScaleOptions(
    val scaleMargins: PriceScaleMargins? = null,
) : IsJsonElement {

    override fun toJsonElement(): JsonElement = buildJsonObject {
        scaleMargins?.let { put("scaleMargins", it.toJsonElement()) }
    }
}

data class PriceScaleMargins(
    val top: Number? = null,
    val bottom: Number? = null,
) : IsJsonElement {

    override fun toJsonElement(): JsonElement = buildJsonObject {
        top?.let { put("top", it) }
        bottom?.let { put("bottom", it) }
    }
}
