package com.saurabhsandav.core.ui.profiles

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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

@Composable
internal fun ProfilesWindow(
    onCloseRequest: () -> Unit,
    onSelectProfile: (ProfileId) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember {
        appModule.profilesModule(scope).presenterFactory.build(
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
            onCopyProfile = { id -> state.eventSink(CopyProfile(id)) },
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
    val appModule = LocalAppModule.current
    val presenter = remember {
        appModule.profilesModule(scope).presenterFactory.build(
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
            onCopyProfile = { id -> state.eventSink(CopyProfile(id)) },
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
    val appModule = LocalAppModule.current
    val presenter = remember {
        appModule.profilesModule(scope).presenterFactory.build(
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
        modifier = Modifier.pointerInput(selectedProfileId) {

            if (selectedProfileId != null) return@pointerInput

            awaitEachGesture {
                // Must be PointerEventPass.Initial to observe events before the text field consumes them
                // in the Main pass
                awaitFirstDown(pass = PointerEventPass.Initial)
                val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                if (upEvent != null) {
                    showSelectorDialog = !showSelectorDialog
                }
            }
        },
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
                onCopyProfile = { id -> state.eventSink(CopyProfile(id)) },
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

            val lazyListState = rememberLazyListState()

            LazyColumn(
                state = lazyListState,
            ) {

                items(profiles, key = { it.id }) { profile ->

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

    if (showNewProfileDialog) {

        ProfileFormDialog(
            type = ProfileFormType.New,
            trainingOnly = trainingOnly,
            onCloseRequest = { showNewProfileDialog = false },
        )
    }
}
