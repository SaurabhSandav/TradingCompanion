package com.saurabhsandav.core.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.theme.dimens

@Composable
fun PrimaryOptionsBar(content: @Composable RowScope.() -> Unit) {

    Row(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.containerPadding),
        horizontalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.dimens.rowHorizontalSpacing,
            alignment = Alignment.End,
        ),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
