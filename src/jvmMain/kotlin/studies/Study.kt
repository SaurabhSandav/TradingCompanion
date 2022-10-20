package studies

import androidx.compose.runtime.Composable

internal interface Study {

    @Composable
    fun render()

    interface Factory<T : Study> {

        val name: String

        fun create(): T
    }
}
