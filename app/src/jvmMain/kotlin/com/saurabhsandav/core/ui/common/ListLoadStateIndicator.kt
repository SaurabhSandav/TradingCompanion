package com.saurabhsandav.core.ui.common

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ListLoadStateIndicator(
    state: LoadStateScope.() -> LoadState,
    emptyText: () -> String,
    content: @Composable () -> Unit,
) {

    var isInitialLoad by state { true }
    val currentState = LoadStateScope.state()

    when {
        // Show loading indicator only on initial load
        isInitialLoad && currentState == LoadState.Loading -> {

            val showLoading by produceState(false, currentState) {
                // To prevent loading indicator being shown unnecessarily if data is ready
                delay(200.milliseconds)
                value = currentState == LoadState.Loading
            }

            if (showLoading) CircularProgressIndicator(Modifier.fillMaxSize().wrapContentSize())
        }

        // Empty data
        currentState == LoadState.Empty -> {

            Text(
                modifier = Modifier.fillMaxSize().wrapContentSize(),
                text = emptyText(),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        // Loaded. Show loading indicator on top if data is refreshed.
        else -> {

            isInitialLoad = false

            val showLoading by produceState(false, currentState) {
                // To prevent loading indicator being shown unnecessarily if data is ready
                delay(200.milliseconds)
                value = currentState == LoadState.Loading
            }

            val scrimColor = DrawerDefaults.scrimColor

            Box(propagateMinConstraints = true) {

                // Main content
                content()

                // Loading Scrim and Indicator
                AnimatedVisibility(
                    modifier = Modifier.fillMaxSize(),
                    visible = showLoading,
                    enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                    exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(),
                ) {

                    CircularProgressIndicator(Modifier.background(scrimColor).wrapContentSize())
                }
            }
        }
    }
}

object LoadStateScope {

    fun loading(): LoadState = LoadState.Loading

    fun empty(): LoadState = LoadState.Empty

    fun loaded(): LoadState = LoadState.Loaded
}

enum class LoadState {
    Loading,
    Empty,
    Loaded,
}
