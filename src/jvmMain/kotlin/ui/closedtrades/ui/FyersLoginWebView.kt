package ui.closedtrades.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ui.common.*

@Composable
internal fun FyersLoginWindow(
    loginUrl: String,
    onLoginSuccess: (redirectUrl: String) -> Unit,
    onCloseRequest: () -> Unit,
) {

    AppWindow(
        onCloseRequest = onCloseRequest,
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
                    webViewState.load(loginUrl)

                    // Watch for successful redirect
                    webViewState.location.collect { newLocation ->
                        if (newLocation.startsWith("http://localhost:8080")) {
                            onLoginSuccess(newLocation)
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
