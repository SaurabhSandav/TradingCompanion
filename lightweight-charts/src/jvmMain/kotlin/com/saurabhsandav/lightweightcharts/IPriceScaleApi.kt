package com.saurabhsandav.lightweightcharts

import com.saurabhsandav.lightweightcharts.data.IRange
import com.saurabhsandav.lightweightcharts.utils.LwcJson
import kotlinx.serialization.Serializable

class IPriceScaleApi(
    receiver: String,
    private val executeJs: (String) -> Unit,
    private val executeJsWithResult: suspend (String) -> String,
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

    fun setVisibleRange(range: IRange<Double>) {
        executeJs("$reference.setVisibleRange(IRange(${range.from}, ${range.to}));")
    }

    suspend fun getVisibleRange(): IRange<Double>? {
        val result = executeJsWithResult("$reference.getVisibleRange();")
        return LwcJson.decodeFromString(result)
    }

    fun setAutoScale(on: Boolean) {
        executeJs("$reference.setAutoScale($on);")
    }
}

@Serializable
data class PriceScaleOptions(
    val autoScale: Boolean? = null,
    val alignLabels: Boolean? = null,
    val scaleMargins: PriceScaleMargins? = null,
) {

    @Serializable
    data class PriceScaleMargins(
        val top: Double? = null,
        val bottom: Double? = null,
    )
}
