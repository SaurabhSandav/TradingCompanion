package chart

interface ChartSeries<T : ChartData> {

    val name: String

    fun setData(list: List<T>)
}
