package com.saurabhsandav.core.chart

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class IPriceScaleApi(
    receiver: String,
    private val executeJs: (String) -> Unit,
    priceScaleId: String? = null,
) {

    private val reference = run {
        val id = if (priceScaleId == null) "" else "'$priceScaleId'"
        "$receiver.priceScale($id)"
    }

    fun applyOptions(options: PriceScaleOptions) {

        val optionsJson = options.toJsonElement()

        executeJs("$reference.applyOptions($optionsJson);")
    }
}

data class PriceScaleOptions(
    val alignLabels: Boolean? = null,
    val scaleMargins: PriceScaleMargins? = null,
) : IsJsonElement {

    override fun toJsonElement(): JsonElement = buildJsonObject {
        alignLabels?.let { put("alignLabels", it) }
        scaleMargins?.let { put("scaleMargins", it.toJsonElement()) }
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
}
