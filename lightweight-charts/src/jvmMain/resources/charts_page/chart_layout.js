function createChartContainer(chartId) {

  let chartDiv = document.createElement("div");

  chartDiv.id = chartId;
  chartDiv.className = "chart";

  document.body.appendChild(chartDiv);

  chartDiv.style.visibility = "hidden";
  chartDiv.style.position = "absolute";
  chartDiv.style.height = "100%";
  chartDiv.style.width = "100%";
  chartDiv.style.top = "0%";
  chartDiv.style.left = "0%";
}

function deleteChartContainer(chartId) {

  let chartDiv = document.getElementById(chartId);

  chartDiv.remove();
}

function getChartContainer(chartId) {
  return document.getElementById(chartId);
}

function hideChart(chartId) {

  let chartDiv = document.getElementById(chartId);

  chartDiv.style.visibility = "hidden";
}

function showChart(chartId) {

  let chartDiv = document.getElementById(chartId);

  chartDiv.style.visibility = "visible";
}

function setChartLayout(chartId, left, top, width, height) {

  let chartDiv = document.getElementById(chartId);

  chartDiv.style.left = left;
  chartDiv.style.top = top;
  chartDiv.style.width = width;
  chartDiv.style.height = height;
}
