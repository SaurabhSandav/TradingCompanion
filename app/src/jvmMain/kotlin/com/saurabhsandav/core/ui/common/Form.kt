package com.saurabhsandav.core.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.saurabhsandav.core.ui.theme.dimens

@Composable
fun Form(
    modifier: Modifier = Modifier,
    width: Dp = FormDefaults.PreferredWidth,
    scrollState: ScrollState? = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {

    Box(
        modifier = modifier,
        propagateMinConstraints = true,
    ) {

        Column(
            modifier = Modifier
                .requiredWidth(width)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .thenIfNotNull(scrollState) { verticalScroll(it) }
                .padding(MaterialTheme.dimens.containerPadding),
            verticalArrangement = Arrangement.spacedBy(
                space = MaterialTheme.dimens.columnVerticalSpacing,
                alignment = Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )

        if (scrollState != null) {

            VerticalScrollbar(
                modifier = Modifier.matchParentSize().wrapContentWidth(Alignment.End),
                adapter = rememberScrollbarAdapter(scrollState),
            )
        }
    }
}

object FormDefaults {

    val PreferredWidth
        get() = MaterialTheme.dimens.formWidth
}
