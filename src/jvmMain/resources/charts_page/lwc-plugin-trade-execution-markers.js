var u = Object.defineProperty;
var h = (i, e, t) => e in i ? u(i, e, { enumerable: !0, configurable: !0, writable: !0, value: t }) : i[e] = t;
var s = (i, e, t) => (h(i, typeof e != "symbol" ? e + "" : e, t), t);
const d = {
  buyFillColor: "green",
  buyTextColor: "white",
  sellFillColor: "red",
  sellTextColor: "white"
};
class c {
  constructor(e, t, r, o, l) {
    s(this, "_points");
    s(this, "_buyFillColor");
    s(this, "_buyTextColor");
    s(this, "_sellFillColor");
    s(this, "_sellTextColor");
    this._points = e, this._buyFillColor = t, this._buyTextColor = r, this._sellFillColor = o, this._sellTextColor = l;
  }
  draw(e) {
    e.useBitmapCoordinateSpace((t) => {
      this._points.forEach((r) => {
        if (r === null)
          return;
        const o = t.context;
        this.drawCircle(o, r);
      });
    });
  }
  drawCircle(e, t) {
    const o = t.side == "buy" ? this._buyFillColor : this._sellFillColor, l = t.side == "buy" ? this._buyTextColor : this._sellTextColor, a = t.side == "buy" ? "B" : "S";
    e.beginPath(), e.fillStyle = o, e.arc(t.p.x, t.p.y, 8, 0, 2 * Math.PI, !1), e.fill(), e.font = "6pt Calibri", e.fillStyle = l, e.textAlign = "center", e.fillText(a, t.p.x, t.p.y + 3);
  }
}
class _ {
  constructor(e) {
    s(this, "_source");
    s(this, "_points", []);
    this._source = e;
  }
  update() {
    const e = this._source.chart, t = this._source.series, r = e.timeScale();
    this._points = this._source.executions.map((o) => {
      const l = r.timeToCoordinate(o.time), a = t.priceToCoordinate(o.price);
      return l != null && a != null ? { p: { x: l, y: a }, side: o.side } : null;
    });
  }
  renderer() {
    return new c(
      this._points,
      this._source.options.buyFillColor,
      this._source.options.buyTextColor,
      this._source.options.sellFillColor,
      this._source.options.sellTextColor
    );
  }
}
function n(i) {
  if (i === void 0)
    throw new Error("Value is undefined");
  return i;
}
class p {
  constructor() {
    s(this, "_chart");
    s(this, "_series");
    s(this, "_requestUpdate");
  }
  requestUpdate() {
    this._requestUpdate && this._requestUpdate();
  }
  attached({
    chart: e,
    series: t,
    requestUpdate: r
  }) {
    this._chart = e, this._series = t, this._series.subscribeDataChanged(this._fireDataUpdated), this._requestUpdate = r, this.requestUpdate();
  }
  detached() {
    this._chart = void 0, this._series = void 0, this._requestUpdate = void 0;
  }
  get chart() {
    return n(this._chart);
  }
  get series() {
    return n(this._series);
  }
  _fireDataUpdated(e) {
    this.dataUpdated && this.dataUpdated(e);
  }
}
class f extends p {
  constructor(t = {}) {
    super();
    s(this, "_options");
    s(this, "_executions", []);
    s(this, "_paneViews");
    this._options = {
      ...d,
      ...t
    }, this._paneViews = [new _(this)];
  }
  updateAllViews() {
    this._paneViews.forEach((t) => t.update());
  }
  paneViews() {
    return this._paneViews;
  }
  get options() {
    return this._options;
  }
  applyOptions(t) {
    this._options = { ...this._options, ...t }, this.requestUpdate();
  }
  set executions(t) {
    this._executions = t, this.requestUpdate();
  }
  get executions() {
    return this._executions;
  }
}
export {
  f as TradeExecutionMarkers
};
