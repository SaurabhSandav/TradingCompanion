package chart.series

import chart.series.data.ChartData
import chart.pricescale.IPriceScaleApi
import kotlinx.serialization.json.Json

interface ISeriesApi<T : ChartData> {

    val name: String

    val priceScale: IPriceScaleApi

    fun setData(list: List<T>)
}

abstract class ISeriesApiImpl<T : ChartData>(
    executeJs: (String) -> Unit,
    json: Json,
    final override val name: String,
) : ISeriesApi<T> {

    override val priceScale: IPriceScaleApi = IPriceScaleApi(name, executeJs, json)
}
