package com.saurabhsandav.core.ui.stats.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.stats.model.StatsState.StatsCategory
import com.saurabhsandav.core.ui.stats.studies.Study

@Composable
internal fun LoadedStats(
    statsCategories: List<StatsCategory>,
    studyFactories: List<Study.Factory<*>>,
    onOpenStudy: (Study.Factory<*>) -> Unit,
) {

    var selectedTabIndex by state { 0 }

    Column {

        TabRow(
            selectedTabIndex = selectedTabIndex,
        ) {

            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Stats") },
            )

            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Studies") },
            )
        }

        Crossfade(selectedTabIndex) { targetTabIndex ->

            when (targetTabIndex) {
                0 -> StatsGrid(statsCategories = statsCategories)
                1 -> StudiesList(
                    studyFactories = studyFactories,
                    onOpenStudy = onOpenStudy,
                )
            }
        }
    }
}

@Composable
internal fun StatsGrid(statsCategories: List<StatsCategory>) {

    Box {

        val lazyGridState = rememberLazyGridState()

        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = lazyGridState,
            columns = GridCells.Adaptive(600.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {

            statsCategories.forEach { statsCategory ->

                item(span = { GridItemSpan(maxLineSpan) }) {

                    StatHeader(statsCategory.label)
                }

                items(
                    items = statsCategory.entries,
                ) { statEntry ->

                    StatItem(
                        label = statEntry.label,
                        value = statEntry.value,
                    )
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(lazyGridState),
        )
    }
}

@Composable
private fun StatHeader(header: String) {

    Column {

        Text(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            text = header,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )

        HorizontalDivider()
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
) {

    Column {

        Row(
            modifier = Modifier.clickable { }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            Text(
                modifier = Modifier.weight(1F),
                text = label,
                textAlign = TextAlign.Center,
            )

            Text(
                modifier = Modifier.weight(1F),
                text = value,
                textAlign = TextAlign.Center,
            )
        }

        HorizontalDivider()
    }
}

@Composable
internal fun StudiesList(
    studyFactories: List<Study.Factory<*>>,
    onOpenStudy: (Study.Factory<*>) -> Unit,
) {

    Box {

        val lazyListState = rememberLazyListState()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
        ) {

            items(items = studyFactories) { studyFactory ->

                ListItem(
                    modifier = Modifier.clickable { onOpenStudy(studyFactory) },
                    headlineContent = { Text(studyFactory.name) },
                )
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(lazyListState),
        )
    }
}
