package com.saurabhsandav.core.ui.tags.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.PrimaryOptionsBar
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tags.form.TagFormDialog
import com.saurabhsandav.core.ui.tags.form.model.TagFormType
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.tags.screen.ui.TagsList

@Composable
fun TagsScreen(
    profileId: ProfileId,
    tags: List<TradeTag>?,
    onDeleteTag: (TradeTagId) -> Unit,
) {

    var formType by state<TagFormType?> { null }

    Scaffold { paddingValues ->

        Column(Modifier.padding(paddingValues)) {

            PrimaryOptionsBar {

                Button(
                    onClick = { formType = TagFormType.New() },
                    shape = MaterialTheme.shapes.small,
                    content = { Text("New Tag") },
                )
            }

            HorizontalDivider()

            TagsList(
                tags = tags,
                onNewTagFromExisting = { tagId -> formType = TagFormType.NewFromExisting(tagId) },
                onEditTag = { tagId -> formType = TagFormType.Edit(tagId) },
                onDeleteTag = onDeleteTag,
            )
        }
    }

    val currentFormType = formType
    if (currentFormType != null) {

        TagFormDialog(
            onCloseRequest = { formType = null },
            profileId = profileId,
            formType = currentFormType,
        )
    }
}
