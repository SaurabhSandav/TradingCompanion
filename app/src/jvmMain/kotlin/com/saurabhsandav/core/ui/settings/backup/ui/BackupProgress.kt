package com.saurabhsandav.core.ui.settings.backup.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.saurabhsandav.core.backup.BackupItem
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.Progress
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.Progress.GeneratingArchive
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.Progress.SavingArchive
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun BackupProgressDialog(progress: Progress) {

    Dialog(onDismissRequest = {}) {

        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
        ) {

            Column(
                modifier = Modifier
                    .padding(MaterialTheme.dimens.containerPadding)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
            ) {

                when (progress) {
                    is GeneratingArchive -> {

                        Text("Generating Archive")

                        val subtitle = when (progress.item) {
                            BackupItem.Prefs -> "Prefs"
                            BackupItem.AppDb -> "App Database"
                            BackupItem.TradingRecords -> "Trading Records"
                            BackupItem.Candles -> "Candles Database"
                        }

                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            progress = { progress.progress },
                        )
                    }

                    SavingArchive -> {

                        Text("Saving Archive")

                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
