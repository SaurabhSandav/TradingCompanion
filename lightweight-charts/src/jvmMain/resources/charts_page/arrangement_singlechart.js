
function prepareSingleChartContainer(chartId) {
  // Chart
  let chartDiv = document.createElement('div');
  chartDiv.id = chartId;
  document.body.appendChild(chartDiv);
  chartDiv.style.position = 'absolute';
  chartDiv.style.height = '100%';
  chartDiv.style.width = '100%';
  chartDiv.addEventListener("mouseenter", (e) => {
    chartCallback(
      JSON.stringify(new ChartCallback(
        chartId,
        "ChartInteraction",
        "mouseenter",
      ))
    );
  });
}
