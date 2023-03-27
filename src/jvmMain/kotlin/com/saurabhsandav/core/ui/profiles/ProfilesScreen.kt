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
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.profiles.model.ProfileModel
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
            onCreateProfile = { profileModel -> presenter.event(CreateNewProfile(profileModel)) },
            onSetCurrent = { id -> presenter.event(SetCurrentProfile(id)) },
            onDeleteProfile = { id -> presenter.event(DeleteProfile(id)) },
            onUpdateProfile = { id, profileModel -> presenter.event(UpdateProfile(id, profileModel)) },
            onCopyProfile = { id -> presenter.event(CopyProfile(id)) },
        )
    }
}

@Composable
internal fun ProfilesScreen(
    profiles: ImmutableList<Profile>,
    onCreateProfile: (ProfileModel) -> Unit,
    onSetCurrent: (Long) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onUpdateProfile: (Long, ProfileModel) -> Unit,
    onCopyProfile: (Long) -> Unit,
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
                            isCurrent = profile.isCurrent,
                            onSetCurrent = { onSetCurrent(profile.id) },
                            onDelete = { onDeleteProfile(profile.id) },
                            onUpdateProfile = { profileModel -> onUpdateProfile(profile.id, profileModel) },
                            onCopyProfile = { onCopyProfile(profile.id) },
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
            profileModel = null,
            onSaveProfile = onCreateProfile,
            onCloseRequest = { showNewProfileDialog = false },
        )
    }
}
