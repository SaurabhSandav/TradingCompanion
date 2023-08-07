package com.saurabhsandav.core.ui.fyerslogin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.JavaFxWebView
import com.saurabhsandav.core.ui.common.WebViewState
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.rememberWebViewState
import com.saurabhsandav.core.ui.common.state

@Composable
internal fun FyersLoginWindow(
    loginState: FyersLoginState,
) {

    AppWindow(
        title = "Login to Fyers",
        onCloseRequest = loginState.onCloseRequest,
    ) {

        Column(Modifier.fillMaxSize()) {

            val webViewState = rememberWebViewState()

            WebViewLoadingIndicator(webViewState)

            JavaFxWebView(
                state = webViewState,
                modifier = Modifier.fillMaxSize(),
            )

            if (webViewState.isReady) {

                // Load login page if WebView is ready
                LaunchedEffect(Unit) {

                    // Load login page
                    webViewState.load(loginState.url)

                    // Watch for successful redirect
                    webViewState.location.collect { newLocation ->
                        if (newLocation.startsWith("http://127.0.0.1:8080")) {
                            loginState.onLoginSuccess(newLocation)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebViewLoadingIndicator(webViewState: WebViewState) {

    var isLoading by state { true }

    if (isLoading) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }

    LaunchedEffect(webViewState.loadState) {
        webViewState.loadState.collect {
            isLoading = it != WebViewState.LoadState.LOADED
        }
    }
}
