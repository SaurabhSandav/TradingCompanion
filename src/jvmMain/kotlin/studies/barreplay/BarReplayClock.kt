package studies.barreplay

internal class BarReplayClock {

    var currentOffset = 0
        private set

    fun next() {
        currentOffset++
    }

    fun reset() {
        currentOffset = 0
    }
}
