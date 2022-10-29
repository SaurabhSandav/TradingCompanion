package studies.barreplay

internal class BarReplayControls {

    private var barReplay: BarReplay? = null
    fun setBarReplay(barReplay: BarReplay) {
        this.barReplay = barReplay
    }

    fun next() {
        barReplay?.next()
    }

    fun reset() {
        barReplay?.reset()
    }

    fun setIsAutoNextEnabled(value: Boolean) {
        barReplay?.isAutoNextEnabled = value
    }

    fun newSymbol(symbol: String) {
        barReplay?.newSymbol(symbol)
    }
}
