package com.saurabhsandav.core.ui.profiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.LocalScreensModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.onTextFieldClickOrEnter
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.profiles.form.ProfileFormDialog
import com.saurabhsandav.core.ui.profiles.form.ProfileFormType
import com.saurabhsandav.core.ui.profiles.model.ProfilesEvent.*
import com.saurabhsandav.core.ui.profiles.model.ProfilesState.Profile
import com.saurabhsandav.core.ui.profiles.ui.ProfilesList

@Composable
internal fun ProfilesWindow(
    onCloseRequest: () -> Unit,
    onSelectProfile: (ProfileId) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val screensModule = LocalScreensModule.current
    val presenter = remember {
        screensModule.profilesModule(scope).presenterFactory.build(
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
            onSelectProfile = onSelectProfile,
            currentProfileId = state.currentProfile?.id,
            onSetCurrentProfile = { id -> state.eventSink(SetCurrentProfile(id)) },
            onDeleteProfile = { id -> state.eventSink(DeleteProfile(id)) },
        )
    }
}

@Composable
fun ProfileSelectorDialog(
    onCloseRequest: () -> Unit,
    selectedProfileId: ProfileId?,
    onProfileSelected: (ProfileId?) -> Unit,
    trainingOnly: Boolean = false,
) {

    val scope = rememberCoroutineScope()
    val screensModule = LocalScreensModule.current
    val presenter = remember {
        screensModule.profilesModule(scope).presenterFactory.build(
            customSelectionMode = true,
            trainingOnly = trainingOnly,
            selectedProfileId = selectedProfileId,
            onProfileSelected = { id ->
                onProfileSelected(id)
                onCloseRequest()
            },
        )
    }
    val state by presenter.state.collectAsState()

    LaunchedEffect(selectedProfileId) {
        state.eventSink(UpdateSelectedProfile(selectedProfileId))
    }

    AppDialogWindow(
        title = "Select Profile",
        onCloseRequest = onCloseRequest,
    ) {

        ProfilesScreen(
            profiles = state.profiles,
            onSelectProfile = { id -> state.eventSink(SetCurrentProfile(id)) },
            currentProfileId = state.currentProfile?.id,
            onSetCurrentProfile = { id -> state.eventSink(SetCurrentProfile(id)) },
            onDeleteProfile = { id -> state.eventSink(DeleteProfile(id)) },
            trainingOnly = trainingOnly,
        )
    }
}

@Composable
fun ProfileSelectorField(
    selectedProfileId: ProfileId?,
    onProfileSelected: (ProfileId?) -> Unit,
    trainingOnly: Boolean = false,
) {

    var showSelectorDialog by state { false }

    val scope = rememberCoroutineScope()
    val screensModule = LocalScreensModule.current
    val presenter = remember {
        screensModule.profilesModule(scope).presenterFactory.build(
            customSelectionMode = true,
            trainingOnly = trainingOnly,
            selectedProfileId = selectedProfileId,
            onProfileSelected = { id ->
                onProfileSelected(id)
                showSelectorDialog = false
            },
        )
    }
    val state by presenter.state.collectAsState()

    LaunchedEffect(selectedProfileId) {
        state.eventSink(UpdateSelectedProfile(selectedProfileId))
    }

    OutlinedTextField(
        modifier = Modifier.onTextFieldClickOrEnter { showSelectorDialog = true },
        value = state.currentProfile?.name ?: "",
        onValueChange = {},
        enabled = true,
        readOnly = true,
        label = { Text("Profile") },
        trailingIcon = {
            when {
                selectedProfileId == null -> ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSelectorDialog)

                else -> IconButton(
                    onClick = { onProfileSelected(null) },
                ) {

                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = null,
                    )
                }
            }
        },
        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
    )

    if (showSelectorDialog) {

        AppDialogWindow(
            title = "Select Profile",
            onCloseRequest = { showSelectorDialog = false },
        ) {

            ProfilesScreen(
                profiles = state.profiles,
                onSelectProfile = { id -> state.eventSink(SetCurrentProfile(id)) },
                currentProfileId = state.currentProfile?.id,
                onSetCurrentProfile = { id -> state.eventSink(SetCurrentProfile(id)) },
                onDeleteProfile = { id -> state.eventSink(DeleteProfile(id)) },
                trainingOnly = trainingOnly,
            )
        }
    }
}

@Composable
private fun ProfilesScreen(
    profiles: List<Profile>,
    onSelectProfile: (ProfileId) -> Unit,
    currentProfileId: ProfileId?,
    onSetCurrentProfile: (ProfileId) -> Unit,
    onDeleteProfile: (ProfileId) -> Unit,
    trainingOnly: Boolean = false,
) {

    var shownProfileFormType by state<ProfileFormType?> { null }

    Scaffold {

        ProfilesList(
            profiles = profiles,
            onNewProfile = { shownProfileFormType = ProfileFormType.New },
            onSelectProfile = onSelectProfile,
            currentProfileId = currentProfileId,
            onSetCurrentProfile = onSetCurrentProfile,
            onDeleteProfile = onDeleteProfile,
            onCopyProfile = { id -> shownProfileFormType = ProfileFormType.Copy(id) },
            trainingOnly = trainingOnly,
        )
    }

    val formType = shownProfileFormType
    if (formType != null) {

        ProfileFormDialog(
            type = formType,
            trainingOnly = trainingOnly,
            onCloseRequest = { shownProfileFormType = null },
        )
    }
}
