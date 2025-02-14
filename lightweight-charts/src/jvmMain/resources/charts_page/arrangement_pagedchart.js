
function preparePagedChartContainer(chartId) {
  // Chart
  let chartDiv = document.createElement('div');
  chartDiv.id = chartId;
  chartDiv.className = 'tabcontent';
  document.body.appendChild(chartDiv);
  chartDiv.style.visibility = "hidden";
  chartDiv.style.position = 'absolute';
  chartDiv.style.height = '100%';
  chartDiv.style.width = '100%';
}

function showPagedChart(chartId) {

  let tabcontent = document.getElementsByClassName("tabcontent");

  for (i = 0; i < tabcontent.length; i++) {
    tabcontent[i].style.visibility = "hidden";
  }

  document.getElementById(chartId).style.visibility = "visible";
}
