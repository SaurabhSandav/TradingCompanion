
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

  // Legend
  let legendDiv = document.createElement('div');
  legendDiv.id = 'legend';
  legendDiv.className = 'legendTextColor';
  chartDiv.appendChild(legendDiv);
  legendDiv.style.position = 'absolute';
  legendDiv.style.left = '12px';
  legendDiv.style.top = '12px';
  legendDiv.style.zIndex = '10';
  legendDiv.style.fontSize = '12px';
  legendDiv.style.lineHeight = '18px';
  legendDiv.style.fontWeight = '300';
}

function setSingleLegendTexts(list) {

  let legendDiv = document.getElementById('legend');

  if (list.length != legendDiv.childElementCount) {

    let newElements = Array(list.length).fill().map(function (_, index) {
      let div = document.createElement('div');
      div.id = `legendItem${index}`;
      return div;
    })

    legendDiv.replaceChildren(...newElements);
  }

  list.forEach(function (item, index) {
    document.getElementById(`legendItem${index}`).innerText = item;
  });
}
