var b = Object.defineProperty;
var w = (a, e, t) => e in a ? b(a, e, { enumerable: !0, configurable: !0, writable: !0, value: t }) : a[e] = t;
var o = (a, e, t) => (w(a, typeof e != "symbol" ? e + "" : e, t), t);
class g {
  constructor(e, t, i) {
    o(this, "_source");
    o(this, "_p");
    o(this, "_labelOptions");
    o(this, "_pos", null);
    this._source = e, this._p = t, this._labelOptions = i;
  }
  coordinate() {
    return this._pos ?? -1;
  }
  visible() {
    return this._source.options.showLabels;
  }
  tickVisible() {
    return this._source.options.showLabels;
  }
  textColor() {
    return this._labelOptions.labelTextColor;
  }
  backColor() {
    return this._labelOptions.labelColor;
  }
  movePoint(e) {
    this._p = e, this.update();
  }
}
class c extends g {
  update() {
    const e = this._source.series;
    this._pos = e.priceToCoordinate(this._p.price);
  }
  text() {
    return this._source.options.priceLabelFormatter(this._p.price);
  }
}
const m = {
  //* Define the default values for all the primitive options.
  entryLabelOptions: {
    labelColor: "#787b86",
    labelTextColor: "white"
  },
  stopFillColor: "rgba(244, 67, 54, 0.2)",
  stopLabelOptions: {
    labelColor: "#f44336",
    labelTextColor: "white"
  },
  targetFillColor: "rgba(0, 150, 136, 0.2)",
  targetLabelOptions: {
    labelColor: "#009688",
    labelTextColor: "white"
  },
  exitArrowColor: "#808c94",
  separatorColor: "black",
  showLabels: !0,
  priceLabelFormatter: (a) => a.toFixed(2)
};
function u(a, e, t) {
  const i = Math.round(t * a), s = Math.round(t * e);
  return {
    position: Math.min(i, s),
    length: Math.abs(s - i) + 1
  };
}
class C {
  constructor(e = [], t, i, s, r) {
    o(this, "_tradeCoordinates", []);
    o(this, "_stopFillColor");
    o(this, "_targetFillColor");
    o(this, "_exitArrowColor");
    o(this, "_separatorColor");
    this._tradeCoordinates = e, this._stopFillColor = t, this._targetFillColor = i, this._exitArrowColor = s, this._separatorColor = r;
  }
  draw(e) {
    e.useBitmapCoordinateSpace((t) => {
      this._tradeCoordinates.forEach((i) => {
        i != null && this.drawDiagram(t, i);
      });
    });
  }
  drawDiagram(e, t) {
    const i = e.context, s = u(
      t.entryX,
      t.exitX,
      e.horizontalPixelRatio
    );
    i.save();
    const r = u(
      t.entryY,
      t.stopY,
      e.verticalPixelRatio
    );
    i.fillStyle = this._stopFillColor, i.fillRect(
      s.position,
      r.position,
      s.length,
      r.length
    ), i.restore(), i.save();
    const n = u(
      t.entryY,
      t.targetY,
      e.verticalPixelRatio
    );
    i.fillStyle = this._targetFillColor, i.fillRect(
      s.position,
      n.position,
      s.length,
      n.length
    ), i.restore(), this.drawArrow(i, t.entryX, t.entryY, t.exitX, t.exitY), this.drawSeparator(i, t.entryX, t.exitX, t.entryY);
  }
  drawSeparator(e, t, i, s) {
    e.save(), e.strokeStyle = this._separatorColor, e.lineWidth = 0.2, e.beginPath(), e.moveTo(t, s), e.lineTo(i, s), e.stroke(), e.restore();
  }
  drawArrow(e, t, i, s, r) {
    var n = 10, l = s - t, p = r - i, h = Math.atan2(p, l);
    e.save(), e.strokeStyle = this._exitArrowColor, e.beginPath(), e.setLineDash([5]), e.moveTo(t, i), e.lineTo(s, r), e.stroke(), e.beginPath(), e.setLineDash([0]), e.moveTo(s, r), e.lineTo(s - n * Math.cos(h - Math.PI / 6), r - n * Math.sin(h - Math.PI / 6)), e.moveTo(s, r), e.lineTo(s - n * Math.cos(h + Math.PI / 6), r - n * Math.sin(h + Math.PI / 6)), e.stroke(), e.restore();
  }
}
class P {
  constructor(e) {
    o(this, "_source");
    o(this, "_tradeCoordinates", []);
    this._source = e;
  }
  update() {
    const t = this._source.chart.timeScale(), i = this._source.series;
    this._tradeCoordinates = this._source.trades.map((s) => {
      const r = t.timeToCoordinate(s.entryTime), n = t.timeToCoordinate(s.exitTime), l = i.priceToCoordinate(s.entryPrice), p = i.priceToCoordinate(s.exitPrice), h = i.priceToCoordinate(s.stopPrice), _ = i.priceToCoordinate(s.targetPrice);
      return r == null || l == null || n == null || p == null || h == null || _ == null ? null : {
        entryX: r,
        entryY: l,
        exitX: n,
        exitY: p,
        stopY: h,
        targetY: _
      };
    });
  }
  renderer() {
    return new C(
      this._tradeCoordinates,
      this._source.options.stopFillColor,
      this._source.options.targetFillColor,
      this._source.options.exitArrowColor,
      this._source.options.separatorColor
    );
  }
}
function d(a) {
  if (a === void 0)
    throw new Error("Value is undefined");
  return a;
}
class T {
  constructor() {
    o(this, "_chart");
    o(this, "_series");
    o(this, "_requestUpdate");
  }
  requestUpdate() {
    this._requestUpdate && this._requestUpdate();
  }
  attached({
    chart: e,
    series: t,
    requestUpdate: i
  }) {
    this._chart = e, this._series = t, this._series.subscribeDataChanged(this._fireDataUpdated), this._requestUpdate = i, this.requestUpdate();
  }
  detached() {
    this._chart = void 0, this._series = void 0, this._requestUpdate = void 0;
  }
  get chart() {
    return d(this._chart);
  }
  get series() {
    return d(this._series);
  }
  _fireDataUpdated(e) {
    this.dataUpdated && this.dataUpdated(e);
  }
}
class f extends T {
  constructor(t = {}) {
    super();
    o(this, "_options");
    o(this, "_trades", []);
    o(this, "_paneViews");
    o(this, "_priceAxisViews", []);
    this._options = {
      ...m,
      ...t
    }, this._paneViews = [new P(this)];
  }
  updateAllViews() {
    this._paneViews.forEach((t) => t.update()), this._priceAxisViews.forEach((t) => t.update());
  }
  priceAxisViews() {
    return this._priceAxisViews;
  }
  paneViews() {
    return this._paneViews;
  }
  autoscaleInfo(t, i) {
    const s = this._trades.flatMap((r) => this._timeCurrentlyVisible(r.entryTime, t, i) || this._timeCurrentlyVisible(r.exitTime, t, i) ? [{
      priceRange: {
        minValue: Math.min(r.entryPrice, r.exitPrice, r.stopPrice, r.targetPrice),
        maxValue: Math.max(r.entryPrice, r.exitPrice, r.stopPrice, r.targetPrice)
      }
    }] : []);
    if (s.length > 0) {
      let r = Number.MAX_VALUE, n = Number.MIN_VALUE;
      return s.forEach((l) => {
        r = Math.min(r, l.priceRange.minValue), n = Math.max(n, l.priceRange.maxValue);
      }), {
        priceRange: { minValue: r, maxValue: n }
      };
    }
    return null;
  }
  _timeCurrentlyVisible(t, i, s) {
    const r = this.chart.timeScale(), n = r.timeToCoordinate(t);
    if (n === null)
      return !1;
    const l = r.coordinateToLogical(n);
    return l === null ? !1 : l <= s && l >= i;
  }
  get options() {
    return this._options;
  }
  applyOptions(t) {
    this._options = { ...this._options, ...t }, this.requestUpdate();
  }
  set trades(t) {
    this._trades = t, this._priceAxisViews = t.flatMap((i) => [
      new c(this, { time: i.entryTime, price: i.entryPrice }, this._options.entryLabelOptions),
      new c(this, { time: i.entryTime, price: i.stopPrice }, this._options.stopLabelOptions),
      new c(this, { time: i.entryTime, price: i.targetPrice }, this._options.targetLabelOptions)
    ]), this.requestUpdate();
  }
  get trades() {
    return this._trades;
  }
}
export {
  f as TradeMarkers
};
