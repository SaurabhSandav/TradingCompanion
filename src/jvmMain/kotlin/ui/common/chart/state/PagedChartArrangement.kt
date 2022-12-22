package ui.common.chart.state

fun ChartArrangement.Companion.paged(): PagedChartArrangement {
    return PagedChartArrangement()
}

class PagedChartArrangement internal constructor() : ChartArrangement() {

    private val pages = mutableSetOf<String>()

    fun addPage(name: String): ChartContainer {

        // Error if chart name already exists
        check(!pages.any { it == name })

        // Create hidden div for new chart
        executeJs(
            """|
            |(function() {
            |  var iDiv = document.createElement('div');
            |  iDiv.id = '$name';
            |  iDiv.className = 'tabcontent';
            |  iDiv.style.display = "none";
            |  document.body.appendChild(iDiv);
            |})()
            """.trimMargin()
        )

        // Add to tabs
        pages += name

        return ChartContainer("document.getElementById('$name')")
    }

    fun removePage(name: String) {

        // Delete chart div
        executeJs("document.getElementById('$name').remove();")

        // Remove tab
        pages.remove(name)
    }

    fun showPage(name: String) {

        // Hide all chart divs, then show selected chart div
        executeJs(
            """|
            |(function() {
            |  var tabcontent = document.getElementsByClassName("tabcontent");
            |  for (i = 0; i < tabcontent.length; i++) {
            |    tabcontent[i].style.display = "none";
            |  }
            |  
            |  document.getElementById('$name').style.display = "block";
            |})()
            """.trimMargin()
        )
    }
}
