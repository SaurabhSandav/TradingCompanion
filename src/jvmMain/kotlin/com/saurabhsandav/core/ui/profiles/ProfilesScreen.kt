package com.saurabhsandav.core.ui.profiles

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.profiles.form.ProfileFormDialog
import com.saurabhsandav.core.ui.profiles.form.ProfileFormType
import com.saurabhsandav.core.ui.profiles.model.ProfilesEvent.*
import com.saurabhsandav.core.ui.profiles.model.ProfilesState.Profile
import com.saurabhsandav.core.ui.profiles.ui.ProfileListItem
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ProfilesWindow(
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember {
        appModule.profilesModule(scope).presenter(
            customSelectionMode = false,
            trainingOnly = false,
        )
    }
    val state by presenter.state.collectAsState()

    AppWindow(
        title = "Profiles",
        onCloseRequest = onCloseRequest,
    ) {

        ProfilesScreen(
            profiles = state.profiles,
            currentProfileId = state.currentProfile?.id,
            onSetCurrentProfile = { id -> state.eventSink(SetCurrentProfile(id)) },
            onDeleteProfile = { id -> state.eventSink(DeleteProfile(id)) },
            onCopyProfile = { id -> state.eventSink(CopyProfile(id)) },
        )
    }
}

@Composable
internal fun ProfileSwitcherBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedProfileId: ProfileId?,
    onSelectProfile: (ProfileId) -> Unit,
    trainingOnly: Boolean = false,
    content: @Composable (String?) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember {
        appModule.profilesModule(scope).presenter(
            customSelectionMode = true,
            trainingOnly = trainingOnly,
        )
    }
    val state by presenter.state.collectAsState()

    LaunchedEffect(selectedProfileId) {
        if (selectedProfileId != null)
            state.eventSink(SetCurrentProfile(selectedProfileId))
    }

    content(state.currentProfile?.name)

    if (expanded) {

        AppDialogWindow(
            title = "Select Profile",
            onCloseRequest = { onExpandedChange(false) },
        ) {

            ProfilesScreen(
                profiles = state.profiles,
                currentProfileId = state.currentProfile?.id,
                onSetCurrentProfile = { id ->
                    onSelectProfile(id)
                    onExpandedChange(false)
                },
                onDeleteProfile = { id -> state.eventSink(DeleteProfile(id)) },
                onCopyProfile = { id -> state.eventSink(CopyProfile(id)) },
                trainingOnly = trainingOnly,
            )
        }
    }
}

@Composable
private fun ProfilesScreen(
    profiles: ImmutableList<Profile>,
    currentProfileId: ProfileId?,
    onSetCurrentProfile: (ProfileId) -> Unit,
    onDeleteProfile: (ProfileId) -> Unit,
    onCopyProfile: (ProfileId) -> Unit,
    trainingOnly: Boolean = false,
) {

    var showNewProfileDialog by state { false }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showNewProfileDialog = true }) {
                Text(text = "New Profile")
            }
        },
    ) {

        Box {

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier.verticalScroll(scrollState).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {

                profiles.forEach { profile ->

                    key(profile.id) {

                        ProfileListItem(
                            profile = profile,
                            isCurrent = profile.id == currentProfileId,
                            onSetCurrentProfile = { onSetCurrentProfile(profile.id) },
                            onDeleteProfile = { onDeleteProfile(profile.id) },
                            onCopyProfile = { onCopyProfile(profile.id) },
                            trainingOnly = trainingOnly,
                        )
                    }
                }
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState)
            )
        }
    }

    if (showNewProfileDialog) {

        ProfileFormDialog(
            type = ProfileFormType.New,
            trainingOnly = trainingOnly,
            onCloseRequest = { showNewProfileDialog = false },
        )
    }
}
