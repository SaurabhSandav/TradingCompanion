package com.saurabhsandav.lightweightcharts

import com.saurabhsandav.lightweightcharts.options.PriceLineOptions
import com.saurabhsandav.lightweightcharts.utils.LwcJson

class IPriceLine internal constructor(
    private val executeJs: (String) -> Unit,
    val id: Int,
    val reference: String,
) {

    fun applyOptions(options: PriceLineOptions) {

        val optionsJson = LwcJson.encodeToString(options)

        executeJs("$reference.applyOptions($optionsJson);")
    }
}
