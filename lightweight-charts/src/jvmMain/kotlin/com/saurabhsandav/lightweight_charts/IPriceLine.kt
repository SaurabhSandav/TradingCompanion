package com.saurabhsandav.lightweight_charts

import com.saurabhsandav.lightweight_charts.options.PriceLineOptions
import com.saurabhsandav.lightweight_charts.utils.LwcJson

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
