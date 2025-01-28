package com.saurabhsandav.core.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.form.FormField

@Composable
fun FormField<*>.errorsMessagesAsSupportingText(): (@Composable () -> Unit)? {

    errorMessages.ifEmpty { return null }

    return {

        Column(Modifier.animateContentSize()) {
            errorMessages.forEach { Text(it) }
        }
    }
}
