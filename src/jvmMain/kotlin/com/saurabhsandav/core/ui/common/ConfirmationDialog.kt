package com.saurabhsandav.core.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ConfirmationDialog(
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        },
    )
}

@Composable
fun DeleteConfirmationDialog(
    subject: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {

    ConfirmationDialog(
        text = "Are you sure you want to delete the $subject?",
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}
