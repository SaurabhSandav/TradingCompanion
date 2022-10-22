package ui.candledownload.ui

import AppDensityFraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import ui.common.JavaFxWebView
import ui.common.WebViewState
import ui.common.rememberWebViewState
import ui.common.state

@Composable
internal fun FyersLoginWebView(
    loginUrl: String,
    onLoginSuccess: (redirectUrl: String) -> Unit,
) {

    val density = LocalDensity.current

    val newDensity = Density(density.density * AppDensityFraction, density.fontScale)

    CompositionLocalProvider(LocalDensity provides newDensity) {

        Column(Modifier.fillMaxSize()) {

            val webViewState = rememberWebViewState()

            WebViewLoadingIndicator(webViewState)

            JavaFxWebView(
                state = webViewState,
                modifier = Modifier.fillMaxSize(0.8F),
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
