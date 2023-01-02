import androidx.compose.runtime.Composable

@Composable
inline fun <T : Any> optionalContent(
    value: T?,
    crossinline content: @Composable (T) -> Unit,
): @Composable (() -> Unit)? = value?.let { { content(it) } }
