package ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ErrorSnackbar(
    snackbarHostState: SnackbarHostState,
    errorMessage: UIErrorMessage,
) {

    LaunchedEffect(errorMessage) {

        val result = snackbarHostState.showSnackbar(
            message = errorMessage.message,
            actionLabel = errorMessage.actionLabel,
            withDismissAction = errorMessage.withDismissAction,
            duration = when (errorMessage.duration) {
                UIErrorMessage.Duration.Indefinite -> SnackbarDuration.Indefinite
                UIErrorMessage.Duration.Long -> SnackbarDuration.Long
                UIErrorMessage.Duration.Short -> SnackbarDuration.Short
            }
        )

        when (result) {
            SnackbarResult.ActionPerformed -> errorMessage.onActionClick?.invoke()
            SnackbarResult.Dismissed -> errorMessage.onDismiss?.invoke()
        }

        errorMessage.onNotified?.invoke(errorMessage)
    }
}

class UIErrorMessage(
    val message: String,
    val actionLabel: String? = null,
    val onActionClick: (() -> Unit)? = null,
    val withDismissAction: Boolean = false,
    val onDismiss: (() -> Unit)? = null,
    val duration: Duration = Duration.Short,
    val onNotified: ((UIErrorMessage) -> Unit)? = null,
) {

    enum class Duration {
        Indefinite,
        Long,
        Short;
    }
}
