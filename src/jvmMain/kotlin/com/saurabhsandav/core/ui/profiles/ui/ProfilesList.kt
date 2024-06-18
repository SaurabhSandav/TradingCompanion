package com.saurabhsandav.core.ui.profiles.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.PrimaryOptionsBar
import com.saurabhsandav.core.ui.profiles.model.ProfilesState.Profile

@Composable
internal fun ProfilesList(
    profiles: List<Profile>,
    onNewProfile: () -> Unit,
    onSelectProfile: (ProfileId) -> Unit,
    currentProfileId: ProfileId?,
    onSetCurrentProfile: (ProfileId) -> Unit,
    onDeleteProfile: (ProfileId) -> Unit,
    onCopyProfile: (ProfileId) -> Unit,
    trainingOnly: Boolean,
) {

    Box {

        val lazyListState = rememberLazyListState()

        LazyColumn(
            state = lazyListState,
        ) {

            stickyHeader {

                Header(
                    onNewProfile = onNewProfile,
                )
            }

            items(
                items = profiles,
                key = { it.id },
            ) { profile ->

                ProfileListItem(
                    profile = profile,
                    onSelectProfile = { onSelectProfile(profile.id) },
                    isCurrent = profile.id == currentProfileId,
                    onSetCurrentProfile = { onSetCurrentProfile(profile.id) },
                    onDeleteProfile = { onDeleteProfile(profile.id) },
                    onCopyProfile = { onCopyProfile(profile.id) },
                    trainingOnly = trainingOnly,
                )
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(lazyListState)
        )
    }
}

@Composable
private fun Header(onNewProfile: () -> Unit) {

    Surface {

        Column {

            PrimaryOptionsBar {

                Button(
                    onClick = onNewProfile,
                    shape = MaterialTheme.shapes.small,
                    content = { Text("New Profile") },
                )
            }

            HorizontalDivider()
        }
    }
}
