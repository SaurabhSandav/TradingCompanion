package com.saurabhsandav.core.ui.fyerslogin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.webview.JavaFxWebView
import com.saurabhsandav.core.ui.common.webview.WebView

@Composable
internal fun FyersLoginWindow(
    loginState: FyersLoginState,
) {

    AppWindow(
        title = "Login to Fyers",
        onCloseRequest = loginState.onCloseRequest,
    ) {

        Column(Modifier.fillMaxSize()) {

            val webView = remember { JavaFxWebView() }

            WebViewLoadingIndicator(webView)

            WebView(
                webView = webView,
                modifier = Modifier.fillMaxSize(),
            )

            // Load login page if WebView is ready
            LaunchedEffect(webView) {

                // Wait for WebView initialization
                webView.awaitReady()

                // Load login page
                webView.load(loginState.url)

                // Watch for successful redirect
                webView.location.collect { newLocation ->
                    if (newLocation.startsWith("http://127.0.0.1:8080")) {
                        loginState.onLoginSuccess(newLocation)
                    }
                }
            }
        }
    }
}

@Composable
private fun WebViewLoadingIndicator(webView: WebView) {

    var isLoading by state { true }

    if (isLoading) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }

    LaunchedEffect(webView.loadState) {
        webView.loadState.collect {
            isLoading = it != WebView.LoadState.LOADED
        }
    }
}
