package com.saurabhsandav.core.ui.common.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.saurabhsandav.core.ui.theme.dimens

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    size: DpSize = DpSize.Unspecified,
    content: @Composable () -> Unit,
) {

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {

        Box {

            // To dismiss Dialog by clicking inside padding area
            Box(
                modifier = Modifier.fillMaxSize().clickable(
                    onClick = onDismissRequest,
                    indication = null,
                    interactionSource = null
                )
            )

            Surface(
                modifier = Modifier
                    .padding(MaterialTheme.dimens.containerPadding)
                    .size(size)
                    .align(Alignment.Center),
                content = content,
            )
        }
    }
}
