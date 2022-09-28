package studies

import androidx.compose.runtime.Composable

interface Study {

    val name: String

    @Composable
    fun render()
}
