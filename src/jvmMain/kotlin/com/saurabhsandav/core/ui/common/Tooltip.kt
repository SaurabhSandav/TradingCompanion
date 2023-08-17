package com.saurabhsandav.core.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

// Tooltips from Google don't appear on hover.
// Prefer Jetbrains tooltips until multiplatform support.
@Composable
fun Tooltip(text: String) {

    Surface(
        modifier = Modifier.shadow(4.dp),
        color = MaterialTheme.colorScheme.inverseOnSurface,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(8.dp)
        )
    }
}
