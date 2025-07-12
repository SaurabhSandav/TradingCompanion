package com.saurabhsandav.lightweightcharts

import com.saurabhsandav.lightweightcharts.data.SeriesData
import com.saurabhsandav.lightweightcharts.options.SeriesOptions

class IPaneApi(
    private val iChartApi: IChartApi,
    private val executeJs: (String) -> Unit,
    private val executeJsWithResult: suspend (String) -> String,
    internal val id: String,
    private val paneReference: String,
) {

    suspend fun getHeight(): Int {
        return executeJsWithResult("$paneReference.getHeight();").toInt()
    }

    fun setHeight(height: Int) {
        executeJs("$paneReference.setHeight($height);")
    }

    fun moveTo(paneIndex: Int) {
        executeJs("$paneReference.moveTo($paneIndex);")
    }

    suspend fun paneIndex(): Int {
        return executeJsWithResult("$paneReference.paneIndex();").toInt()
    }

    internal fun setPreserveEmptyPane(preserve: Boolean) {
        executeJs("$paneReference.setPreserveEmptyPane($preserve);")
    }

    suspend fun preserveEmptyPane(): Boolean {
        return executeJsWithResult("$paneReference.preserveEmptyPane();").toBoolean()
    }

    fun setStretchFactor(stretchFactor: Number) {
        executeJs("$paneReference.setStretchFactor($stretchFactor);")
    }

    suspend fun getStretchFactor(preserve: Boolean): Double {
        return executeJsWithResult("$paneReference.getStretchFactor($preserve);").toDouble()
    }

    fun <D : SeriesData, O : SeriesOptions> addSeries(
        definition: SeriesDefinition<D, O>,
        options: O? = null,
    ): ISeriesApi<D, O> {
        return iChartApi.addSeries(definition, options, iChartApi.panesList.indexOf(this))
    }
}
