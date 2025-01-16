package com.saurabhsandav.core.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Stable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@Stable
class UIMessagesState {

    private val messagesChannel = Channel<UIMessage>()
    val messages = messagesChannel.receiveAsFlow()

    suspend fun showMessage(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: UIMessageDuration = when (actionLabel) {
            null -> UIMessageDuration.Short
            else -> UIMessageDuration.Indefinite
        },
    ): UIMessageResult {

        val result = CompletableDeferred<UIMessageResult>()

        val message = UIMessage(
            message = message,
            actionLabel = actionLabel,
            withDismissAction = withDismissAction,
            duration = duration,
            result = result,
        )

        messagesChannel.send(message)

        return result.await()
    }
}

class UIMessage(
    val message: String,
    val actionLabel: String? = null,
    val withDismissAction: Boolean = false,
    val duration: UIMessageDuration = UIMessageDuration.Short,
    val result: CompletableDeferred<UIMessageResult>,
)

enum class UIMessageDuration {
    Indefinite,
    Long,
    Short;
}

enum class UIMessageResult {
    Dismissed,
    ActionPerformed,
}

suspend fun UIMessagesState.showAsSnackbarsIn(
    snackbarHostState: SnackbarHostState,
) {

    messages.collect { uiMessage ->

        val snackbarResult = snackbarHostState.showSnackbar(
            message = uiMessage.message,
            actionLabel = uiMessage.actionLabel,
            withDismissAction = uiMessage.withDismissAction,
            duration = when (uiMessage.duration) {
                UIMessageDuration.Indefinite -> SnackbarDuration.Indefinite
                UIMessageDuration.Long -> SnackbarDuration.Long
                UIMessageDuration.Short -> SnackbarDuration.Short
            }
        )

        val uiMessageResult = when (snackbarResult) {
            SnackbarResult.ActionPerformed -> UIMessageResult.ActionPerformed
            SnackbarResult.Dismissed -> UIMessageResult.Dismissed
        }

        uiMessage.result.complete(uiMessageResult)
    }
}
