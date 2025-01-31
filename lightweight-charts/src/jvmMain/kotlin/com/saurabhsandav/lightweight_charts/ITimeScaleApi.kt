package com.saurabhsandav.lightweight_charts

import com.saurabhsandav.lightweight_charts.callbacks.CallbackDelegate
import com.saurabhsandav.lightweight_charts.callbacks.LogicalRangeChangeEventHandler
import com.saurabhsandav.lightweight_charts.callbacks.SizeChangeEventHandler
import com.saurabhsandav.lightweight_charts.callbacks.TimeRangeChangeEventHandler
import com.saurabhsandav.lightweight_charts.data.IRange
import com.saurabhsandav.lightweight_charts.data.LogicalRange
import com.saurabhsandav.lightweight_charts.data.Time
import com.saurabhsandav.lightweight_charts.options.TimeScaleOptions
import com.saurabhsandav.lightweight_charts.utils.LwcJson

class ITimeScaleApi internal constructor(
    private val receiver: String,
    chartInstanceReference: String,
    private val callbacksDelegate: CallbackDelegate,
    private val executeJs: (String) -> Unit,
    private val executeJsWithResult: suspend (String) -> String,
) {

    private val subscribeVisibleTimeRangeChangeCallbackReference =
        "$chartInstanceReference.subscribeVisibleTimeRangeChangeCallback"
    private val subscribeVisibleLogicalRangeChangeCallbackReference =
        "$chartInstanceReference.subscribeVisibleLogicalRangeChangeCallback"
    private val subscribeSizeChangeCallbackReference = "$chartInstanceReference.subscribeSizeChangeCallback"

    suspend fun scrollPosition(): Float {
        return executeJsWithResult("$receiver.timeScale().scrollPosition()").toFloat()
    }

    fun scrollToPosition(position: Int, animated: Boolean) {
        executeJs("$receiver.timeScale().scrollToPosition($position, $animated);")
    }

    fun scrollToRealTime() {
        executeJs("$receiver.timeScale().scrollToRealTime();")
    }

    suspend fun getVisibleRange(): IRange<Time>? {

        val result = executeJsWithResult("$receiver.timeScale().getVisibleRange()")

        return LwcJson.decodeFromString(result)
    }

    fun setVisibleRange(from: Time, to: Time) {

        val rangeJson = LwcJson.encodeToString(IRange(from = from, to = to))

        executeJs("$receiver.timeScale().setVisibleRange($rangeJson);")
    }

    suspend fun getVisibleLogicalRange(): LogicalRange? {

        val result = executeJsWithResult("$receiver.timeScale().getVisibleLogicalRange()")

        return LwcJson.decodeFromString(result)
    }

    fun setVisibleLogicalRange(from: Float, to: Float) {

        val rangeJson = LwcJson.encodeToString(LogicalRange(from = from, to = to))

        executeJs("$receiver.timeScale().setVisibleLogicalRange($rangeJson);")
    }

    fun resetTimeScale() {
        executeJs("$receiver.timeScale().resetTimeScale();")
    }

    fun fitContent() {
        executeJs("$receiver.timeScale().fitContent();")
    }

    fun subscribeVisibleTimeRangeChange(handler: TimeRangeChangeEventHandler) {

        if (callbacksDelegate.subscribeVisibleTimeRangeChangeCallbacks.isEmpty())
            executeJs("$receiver.timeScale().subscribeVisibleTimeRangeChange($subscribeVisibleTimeRangeChangeCallbackReference);")

        callbacksDelegate.subscribeVisibleTimeRangeChangeCallbacks.add(handler)
    }

    fun unsubscribeVisibleTimeRangeChange(handler: TimeRangeChangeEventHandler) {

        callbacksDelegate.subscribeVisibleTimeRangeChangeCallbacks.remove(handler)

        if (callbacksDelegate.subscribeVisibleTimeRangeChangeCallbacks.isEmpty())
            executeJs("$receiver.timeScale().unsubscribeVisibleTimeRangeChange($subscribeVisibleTimeRangeChangeCallbackReference);")
    }

    fun subscribeVisibleLogicalRangeChange(handler: LogicalRangeChangeEventHandler) {

        if (callbacksDelegate.subscribeVisibleLogicalRangeChangeCallbacks.isEmpty())
            executeJs("$receiver.timeScale().subscribeVisibleLogicalRangeChange($subscribeVisibleLogicalRangeChangeCallbackReference);")

        callbacksDelegate.subscribeVisibleLogicalRangeChangeCallbacks.add(handler)
    }

    fun unsubscribeVisibleLogicalRangeChange(handler: LogicalRangeChangeEventHandler) {

        callbacksDelegate.subscribeVisibleLogicalRangeChangeCallbacks.remove(handler)

        if (callbacksDelegate.subscribeVisibleLogicalRangeChangeCallbacks.isEmpty())
            executeJs("$receiver.timeScale().unsubscribeVisibleLogicalRangeChange($subscribeVisibleLogicalRangeChangeCallbackReference);")
    }

    fun subscribeSizeChange(handler: SizeChangeEventHandler) {

        if (callbacksDelegate.subscribeSizeChangeCallbacks.isEmpty())
            executeJs("$receiver.timeScale().subscribeSizeChange($subscribeSizeChangeCallbackReference);")

        callbacksDelegate.subscribeSizeChangeCallbacks.add(handler)
    }

    fun unsubscribeSizeChange(handler: SizeChangeEventHandler) {

        callbacksDelegate.subscribeSizeChangeCallbacks.remove(handler)

        if (callbacksDelegate.subscribeSizeChangeCallbacks.isEmpty())
            executeJs("$receiver.timeScale().unsubscribeSizeChange($subscribeSizeChangeCallbackReference);")
    }

    fun applyOptions(options: TimeScaleOptions) {

        val optionsJson = LwcJson.encodeToString(options)

        executeJs("$receiver.timeScale().applyOptions($optionsJson);")
    }
}
