var c = Object.defineProperty;
var d = (s, t, e) => t in s ? c(s, t, { enumerable: !0, configurable: !0, writable: !0, value: e }) : s[t] = e;
var r = (s, t, e) => (d(s, typeof t != "symbol" ? t + "" : t, e), e);
const u = {
  lineColor: "#4985E7"
};
class _ {
  constructor(t, e) {
    r(this, "_x");
    r(this, "_lineColor");
    this._x = t, this._lineColor = e;
  }
  draw(t) {
    t.useBitmapCoordinateSpace((e) => {
      this._x.forEach((o) => {
        if (o === null)
          return;
        const i = e.context;
        i.beginPath(), i.setLineDash([5]), i.moveTo(o, 0), i.lineTo(o, e.bitmapSize.height), i.strokeStyle = this._lineColor, i.stroke();
      });
    });
  }
}
class l {
  constructor(t) {
    r(this, "_source");
    r(this, "_x", []);
    this._source = t;
  }
  update() {
    const e = this._source.chart.timeScale();
    this._x = this._source.times.map((o) => {
      const i = e.timeToCoordinate(o), n = i ? e.coordinateToLogical(i) : null, a = n ? e.logicalToCoordinate(n - 1) : null;
      return a != null && i != null ? a + (i - a) / 2 : null;
    });
  }
  renderer() {
    return new _(
      this._x,
      this._source.options.lineColor
    );
  }
  zOrder() {
    return "bottom";
  }
}
function h(s) {
  if (s === void 0)
    throw new Error("Value is undefined");
  return s;
}
class p {
  constructor() {
    r(this, "_chart");
    r(this, "_series");
    r(this, "_requestUpdate");
  }
  requestUpdate() {
    this._requestUpdate && this._requestUpdate();
  }
  attached({
    chart: t,
    series: e,
    requestUpdate: o
  }) {
    this._chart = t, this._series = e, this._series.subscribeDataChanged(this._fireDataUpdated), this._requestUpdate = o, this.requestUpdate();
  }
  detached() {
    this._chart = void 0, this._series = void 0, this._requestUpdate = void 0;
  }
  get chart() {
    return h(this._chart);
  }
  get series() {
    return h(this._series);
  }
  _fireDataUpdated(t) {
    this.dataUpdated && this.dataUpdated(t);
  }
}
class f extends p {
  constructor(e = {}) {
    super();
    r(this, "_options");
    r(this, "_times", []);
    r(this, "_paneViews");
    this._options = {
      ...u,
      ...e
    }, this._paneViews = [new l(this)];
  }
  updateAllViews() {
    this._paneViews.forEach((e) => e.update());
  }
  paneViews() {
    return this._paneViews;
  }
  get options() {
    return this._options;
  }
  applyOptions(e) {
    this._options = { ...this._options, ...e }, this.requestUpdate();
  }
  set times(e) {
    this._times = e, this.requestUpdate();
  }
  get times() {
    return this._times;
  }
}
export {
  f as SessionMarkers
};
