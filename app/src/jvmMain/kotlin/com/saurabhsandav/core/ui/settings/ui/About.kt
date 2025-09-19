package com.saurabhsandav.core.ui.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.BuildKonfig
import com.saurabhsandav.core.ui.theme.dimens
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun About() {

    SelectionContainer {

        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.containerPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
        ) {

            Text("Version: ${BuildKonfig.VERSION}")

            val formattedGitCommitTime = remember {
                LocalDateTime.parse(
                    input = BuildKonfig.VERSION.split('.').take(2).joinToString(""),
                    format = LocalDateTime.Format {
                        year()
                        monthNumber()
                        day()
                        hour()
                        minute()
                        second()
                    },
                ).toInstant(TimeZone.UTC)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .format(DateTimeFormat)
            }

            Text("Last Commit Time: $formattedGitCommitTime")

            val formattedBuildTime = remember {
                Instant.fromEpochMilliseconds(BuildKonfig.BUILD_TIME)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .format(DateTimeFormat)
            }

            Text("Build Time: $formattedBuildTime")
        }
    }
}

private val DateTimeFormat = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_FULL)
    char(' ')
    day()
    chars(", ")
    year()
    chars(" - ")
    hour()
    char(':')
    minute()
    char(':')
    second()
}
