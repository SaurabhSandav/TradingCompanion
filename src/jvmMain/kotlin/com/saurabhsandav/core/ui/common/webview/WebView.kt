package com.saurabhsandav.core.ui.common.webview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.app.AppSwingPanel
import kotlinx.coroutines.flow.Flow
import java.awt.Component

@Composable
fun WebView(
    webView: WebView,
    modifier: Modifier = Modifier,
) {

    AppSwingPanel(
        modifier = modifier,
        factory = { webView.component }
    )

    LaunchedEffect(webView) {

        webView.init()
    }
}

@Stable
interface WebView {

    val component: Component

    val loadState: Flow<LoadState>

    val location: Flow<String>

    val errors: Flow<Throwable>

    suspend fun init()

    suspend fun awaitReady()

    suspend fun load(url: String)

    suspend fun loadResource(path: String)

    suspend fun executeScript(script: String)

    suspend fun createJSCallback(jsFuncName: String): JSCallback

    suspend fun setBackgroundColor(color: androidx.compose.ui.graphics.Color)

    enum class LoadState {
        INITIALIZED,
        LOADING,
        LOADED,
        FAILED;
    }

    interface JSCallback {

        val messages: Flow<String>

        suspend fun remove()
    }
}
