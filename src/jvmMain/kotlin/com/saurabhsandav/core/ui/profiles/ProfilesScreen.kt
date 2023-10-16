package com.saurabhsandav.core.ui.profiles

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.profiles.model.ProfileFormModel
import com.saurabhsandav.core.ui.profiles.model.ProfilesEvent.*
import com.saurabhsandav.core.ui.profiles.model.ProfilesState.Profile
import com.saurabhsandav.core.ui.profiles.ui.ProfileEditorDialog
import com.saurabhsandav.core.ui.profiles.ui.ProfileListItem
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ProfilesWindow(
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { ProfilesPresenter(scope, appModule) }
    val state by presenter.state.collectAsState()

    AppWindow(
        title = "Profiles",
        onCloseRequest = onCloseRequest,
    ) {

        ProfilesScreen(
            profiles = state.profiles,
            currentProfileId = state.currentProfile?.id,
            onCreateProfile = { profileModel -> state.eventSink(CreateProfile(profileModel)) },
            onSetCurrentProfile = { id -> state.eventSink(SetCurrentProfile(id)) },
            onDeleteProfile = { id -> state.eventSink(DeleteProfile(id)) },
            onUpdateProfile = { id, profileModel -> state.eventSink(UpdateProfile(id, profileModel)) },
            onCopyProfile = { id -> state.eventSink(CopyProfile(id)) },
        )
    }
}

@Composable
internal fun ProfileSwitcher(
    selectedProfileId: Long?,
    onSelectProfile: (Long) -> Unit,
    modifier: Modifier = Modifier,
    trainingOnly: Boolean = false,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember {
        ProfilesPresenter(scope, appModule, customSelectionMode = true, trainingOnly = trainingOnly)
    }
    val state by presenter.state.collectAsState()

    var showSelectorDialog by state { false }

    LaunchedEffect(selectedProfileId) {
        if (selectedProfileId != null)
            state.eventSink(SetCurrentProfile(selectedProfileId))
    }

    TextButton(
        modifier = modifier,
        onClick = { showSelectorDialog = true },
    ) {

        Text("Profile: ${state.currentProfile?.name ?: "None"}")
    }

    if (showSelectorDialog) {

        AppDialogWindow(
            title = "Select Profile",
            onCloseRequest = { showSelectorDialog = false },
        ) {

            ProfilesScreen(
                profiles = state.profiles,
                currentProfileId = state.currentProfile?.id,
                onCreateProfile = { profileModel -> state.eventSink(CreateProfile(profileModel)) },
                onSetCurrentProfile = { id ->
                    onSelectProfile(id)
                    showSelectorDialog = false
                },
                onDeleteProfile = { id -> state.eventSink(DeleteProfile(id)) },
                onUpdateProfile = { id, profileModel -> state.eventSink(UpdateProfile(id, profileModel)) },
                onCopyProfile = { id -> state.eventSink(CopyProfile(id)) },
                trainingOnly = trainingOnly,
            )
        }
    }
}

@Composable
private fun ProfilesScreen(
    profiles: ImmutableList<Profile>,
    currentProfileId: Long?,
    onCreateProfile: (ProfileFormModel) -> Unit,
    onSetCurrentProfile: (Long) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onUpdateProfile: (Long, ProfileFormModel) -> Unit,
    onCopyProfile: (Long) -> Unit,
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
                            name = profile.name,
                            description = profile.description,
                            isTraining = profile.isTraining,
                            isCurrent = profile.id == currentProfileId,
                            onSetCurrentProfile = { onSetCurrentProfile(profile.id) },
                            onDeleteProfile = { onDeleteProfile(profile.id) },
                            onUpdateProfile = { profileModel -> onUpdateProfile(profile.id, profileModel) },
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

        ProfileEditorDialog(
            initialModel = null,
            onSaveProfile = onCreateProfile,
            onCloseRequest = { showNewProfileDialog = false },
            trainingOnly = trainingOnly,
        )
    }
}
