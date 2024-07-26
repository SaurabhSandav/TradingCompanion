package com.saurabhsandav.lightweight_charts

import com.saurabhsandav.lightweight_charts.options.PriceLineOptions

class IPriceLine internal constructor(
    private val executeJs: (String) -> Unit,
    val id: Int,
    val reference: String,
) {

    fun applyOptions(options: PriceLineOptions) {

        val optionsJson = options.toJsonElement()

        executeJs("$reference.applyOptions($optionsJson);")
    }
}
