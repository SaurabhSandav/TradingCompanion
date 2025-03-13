package com.saurabhsandav.lightweightcharts

class IPaneApi(
    private val executeJs: (String) -> Unit,
    private val executeJsWithResult: suspend (String) -> String,
    private val paneReference: String,
) {

    suspend fun getHeight(): Int {
        return executeJsWithResult("$paneReference.getHeight();").toInt()
    }

    fun setHeight(height: Int) {
        executeJs("$paneReference.setHeight($height);")
    }

    fun moveTo(paneIndex: Int) {
        executeJs("$paneReference.moveTo($paneIndex);")
    }

    suspend fun paneIndex(): Int {
        return executeJsWithResult("$paneReference.paneIndex();").toInt()
    }
}
