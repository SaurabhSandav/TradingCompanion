
const charts = new Map();

class ChartInstance {

  constructor(id, chart, callbackFunc) {
    this.chart = chart;
    let seriesMap = new Map();
    this.seriesMap = seriesMap;
    this.panesMap = new Map();
    this.subscribeClickCallback = (function (params) {
      callbackFunc(
        JSON.stringify(new ChartCallback(
          id,
          "subscribeClickCallback",
          params,
        ), replacerSeriesByName(seriesMap))
      );
    });
    this.subscribeCrosshairMoveCallback = (function (params) {
      callbackFunc(
        JSON.stringify(new ChartCallback(
          id,
          "subscribeCrosshairMoveCallback",
          params,
        ), replacerSeriesByName(seriesMap))
      );
    });
    this.subscribeVisibleTimeRangeChangeCallback = (function (timeRange) {
      callbackFunc(
        JSON.stringify(new ChartCallback(
          id,
          "subscribeVisibleTimeRangeChangeCallback",
          timeRange,
        ))
      );
    });
    this.subscribeVisibleLogicalRangeChangeCallback = (function (logicalRange) {
      callbackFunc(
        JSON.stringify(new ChartCallback(
          id,
          "subscribeVisibleLogicalRangeChangeCallback",
          logicalRange,
        ))
      );
    });
    this.subscribeSizeChangeCallback = (function (width, height) {
      callbackFunc(
        JSON.stringify(new ChartCallback(
          id,
          "subscribeSizeChangeCallback",
          { width: width, height: height },
        ))
      );
    });
  }
}

class SeriesInstance {

  constructor(series) {
    this.series = series;
    this.pane = null;
    this.priceLinesMap = new Map();
    this.primitivesMap = new Map();
  }
}

class ChartCallback {

  constructor(chartId, callbackType, message) {
    this.chartId = chartId;
    this.callbackType = callbackType;
    this.message = message;
  }
}

function getByValue(map, searchValue) {
  for (let [key, value] of map) {
    if (value.series === searchValue)
      return key;
  }
}

function replacerSeriesByName(seriesMap) {

  return function (key, value) {
    if (key == 'sourceEvent' || key == 'hoveredSeries') {
      return undefined;
    } else if (key == 'seriesData' && value instanceof Map) {

      let namedMap = new Map();

      value.forEach(function (value, key) {
        namedMap.set(getByValue(seriesMap, key), value);
      });

      return Object.fromEntries(namedMap);
    } else {
      return value;
    }
  }
}

let styleSheet;

function setPageBackgroundColor(color) {
  // create stylesheet, if not already created
  if (!styleSheet) {
    const styleElement = document.createElement('style');
    document.head.appendChild(styleElement);
    styleSheet = styleElement.sheet;
  }

  // insert style rule at the end of the stylesheet,
  // overriding existing rules on same selector
  styleSheet.insertRule(
    `body { background-color: ${color} }`,
    styleSheet.cssRules.length
  );
}
