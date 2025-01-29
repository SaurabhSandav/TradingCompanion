package com.saurabhsandav.core.ui.tags.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.saurabhsandav.core.LocalScreensModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.SimpleTooltipBox
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tags.form.TagFormWindow
import com.saurabhsandav.core.ui.tags.form.model.TagFormType

@Composable
internal fun TagSelectorDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    profileId: ProfileId,
    type: () -> TagSelectorType,
    onSelectTag: (TradeTagId) -> Unit,
    modifier: Modifier = Modifier,
    allowCreate: Boolean = true,
) {

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {

        val screensModule = LocalScreensModule.current
        val state = remember { screensModule.tagSelectorStateFactory().build(profileId, type()) }

        val focusRequester = remember { FocusRequester() }

        var showCreateTagWindow by state { false }

        OutlinedTextField(
            modifier = Modifier.focusRequester(focusRequester),
            value = state.filterQuery,
            onValueChange = { state.filterQuery = it },
            singleLine = true,
            trailingIcon = if (!allowCreate) null else {
                { CreateTagButton { showCreateTagWindow = true } }
            },
        )

        if (showCreateTagWindow) {

            TagFormWindow(
                profileId = profileId,
                formType = TagFormType.New(state.filterQuery),
                onCloseRequest = { showCreateTagWindow = false },
            )
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        val tags by state.tags.collectAsState(emptyList())

        tags.forEach { tag ->

            SimpleTooltipBox(tag.description) {

                DropdownMenuItem(
                    text = { Text(tag.name) },
                    trailingIcon = tag.color?.let {
                        { Box(Modifier.size(InputChipDefaults.IconSize).background(tag.color)) }
                    },
                    onClick = {
                        onDismissRequest()
                        onSelectTag(tag.id)
                    },
                )
            }
        }
    }
}

@Composable
internal fun CreateTagButton(onClick: () -> Unit) {

    val text = "Create Tag"

    IconButtonWithTooltip(
        onClick = onClick,
        tooltipText = text,
    ) {

        Icon(Icons.Default.Add, contentDescription = text)
    }
}
