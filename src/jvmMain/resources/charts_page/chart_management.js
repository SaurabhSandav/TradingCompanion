
const charts = new Map();

class ChartInstance {

  constructor(name, chart) {
    this.chart = chart;
    var seriesMap = new Map();
    this.seriesMap = seriesMap;
    this.subscribeClickCallback = (function (params) {
      chartCallback(
        JSON.stringify(new ChartCallback(
          name,
          "subscribeClickCallback",
          params,
        ), replacerSeriesByName(seriesMap))
      );
    })
    this.subscribeCrosshairMoveCallback = (function (params) {
      chartCallback(
        JSON.stringify(new ChartCallback(
          name,
          "subscribeCrosshairMoveCallback",
          params,
        ), replacerSeriesByName(seriesMap))
      );
    })
    this.subscribeVisibleTimeRangeChangeCallback = (function (timeRange) {
      chartCallback(
        JSON.stringify(new ChartCallback(
          name,
          "subscribeVisibleTimeRangeChangeCallback",
          timeRange,
        ))
      );
    })
    this.subscribeVisibleLogicalRangeChangeCallback = (function (logicalRange) {
      chartCallback(
        JSON.stringify(new ChartCallback(
          name,
          "subscribeVisibleLogicalRangeChangeCallback",
          logicalRange,
        ))
      );
    })
    this.subscribeSizeChangeCallback = (function (width, height) {
      chartCallback(
        JSON.stringify(new ChartCallback(
          name,
          "subscribeSizeChangeCallback",
          { width: width, height: height },
        ))
      );
    })
  }
}

class SeriesInstance {

  constructor(series) {
    this.series = series;
    this.priceLinesMap = new Map();
  }
}

class ChartCallback {

  constructor(chartName, callbackType, message) {
    this.chartName = chartName;
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
    if (key == 'seriesPrices' && value instanceof Map) {

      var namedMap = new Map();

      value.forEach(function (value, key) {
        namedMap.set(getByValue(seriesMap, key), value);
      });

      return Array.from(namedMap.entries());
    } else {
      return value;
    }
  }
}
