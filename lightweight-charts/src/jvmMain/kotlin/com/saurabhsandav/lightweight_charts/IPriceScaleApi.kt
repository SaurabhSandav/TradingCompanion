package com.saurabhsandav.lightweight_charts

import com.saurabhsandav.lightweight_charts.utils.LwcJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

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

        val optionsJson = LwcJson.encodeToString(options)

        executeJs("$reference.applyOptions($optionsJson);")
    }
}

@Serializable
data class PriceScaleOptions(
    val alignLabels: Boolean? = null,
    val scaleMargins: PriceScaleMargins? = null,
) {

    @Serializable
    data class PriceScaleMargins(
        val top: Double? = null,
        val bottom: Double? = null,
    )
}
