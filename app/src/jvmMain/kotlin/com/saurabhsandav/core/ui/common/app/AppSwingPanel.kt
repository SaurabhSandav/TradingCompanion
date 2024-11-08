package com.saurabhsandav.core.ui.common.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.NoOpUpdate
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.saurabhsandav.core.LocalAppConfig
import com.saurabhsandav.core.originalDensity
import java.awt.Component

@Composable
fun <T : Component> AppSwingPanel(
    background: Color = MaterialTheme.colorScheme.background,
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate,
) {

    val appConfig = LocalAppConfig.current

    CompositionLocalProvider(LocalDensity provides appConfig.originalDensity()) {

        SwingPanel(
            background = background,
            factory = factory,
            modifier = modifier,
            update = update,
        )
    }
}
