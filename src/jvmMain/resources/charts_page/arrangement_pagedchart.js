
function preparePagedChartContainer(chartId) {
  // Chart
  let chartDiv = document.createElement('div');
  chartDiv.id = chartId;
  chartDiv.className = 'tabcontent';
  document.body.appendChild(chartDiv);
  chartDiv.style.display = "none";
  chartDiv.style.position = 'absolute';

  // Legend
  let legendDiv = document.createElement('div');
  legendDiv.id = `${chartId}_legend`;
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

function showPagedChart(chartId) {

  var tabcontent = document.getElementsByClassName("tabcontent");

  for (i = 0; i < tabcontent.length; i++) {
    tabcontent[i].style.display = "none";
  }

  document.getElementById(chartId).style.display = "block";
}

function setPagedLegendTexts(chartId, list) {

  let legendDiv = document.getElementById(`${chartId}_legend`);

  if (list.length != legendDiv.childElementCount) {

    let newElements = Array(list.length).fill().map(function (_, index) {
      let div = document.createElement('div');
      div.id = `${chartId}_legendItem${index}`;
      return div;
    })

    legendDiv.replaceChildren(...newElements);
  }

  list.forEach(function (item, index) {
    document.getElementById(`${chartId}_legendItem${index}`).innerText = item;
  });
}
