package chart

interface ISeriesApi<T : ChartData> {

    val name: String

    fun setData(list: List<T>)
}
