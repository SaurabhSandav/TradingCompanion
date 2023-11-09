package com.saurabhsandav.core.ui.common.webview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.AwtColor
import com.saurabhsandav.core.ui.common.app.AppSwingPanel
import kotlinx.coroutines.flow.Flow
import java.awt.Component

@Composable
fun WebView(
    webViewState: WebViewState,
    modifier: Modifier = Modifier,
) {

    AppSwingPanel(
        modifier = modifier,
        factory = { webViewState.component }
    )

    LaunchedEffect(webViewState) {

        webViewState.init()
    }
}

@Stable
interface WebViewState {

    val component: Component

    val loadState: Flow<LoadState>

    val location: Flow<String>

    val errors: Flow<Throwable>

    suspend fun init()

    suspend fun awaitReady()

    suspend fun load(url: String)

    suspend fun executeScript(script: String)

    suspend fun createJSCallback(jsFuncName: String): JSCallback

    suspend fun setBackgroundColor(color: AwtColor)

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
