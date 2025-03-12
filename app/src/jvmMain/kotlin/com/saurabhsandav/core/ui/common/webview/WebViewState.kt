package com.saurabhsandav.core.ui.common.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.AwtColor
import kotlinx.coroutines.flow.Flow

@Composable
fun WebView(
    webViewState: WebViewState,
    modifier: Modifier = Modifier,
) = webViewState.WebView(modifier)

interface WebViewState {

    @Composable
    fun WebView(modifier: Modifier)

    val loadState: Flow<LoadState>

    val location: Flow<String>

    val errors: Flow<Throwable>

    suspend fun awaitReady()

    suspend fun load(url: String)

    suspend fun executeScript(script: String)

    suspend fun createJSCallback(jsFuncName: String): JSCallback

    suspend fun setBackgroundColor(color: AwtColor)

    enum class LoadState {
        INITIALIZED,
        LOADING,
        LOADED,
        FAILED,
    }

    interface JSCallback {

        val messages: Flow<String>

        suspend fun remove()
    }
}
