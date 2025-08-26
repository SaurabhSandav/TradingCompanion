package com.saurabhsandav.core.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.LocalAppGraph
import com.saurabhsandav.core.ui.common.BoxWithScrollbar
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.settings.backup.BackupPreferencesPane
import com.saurabhsandav.core.ui.settings.model.SettingsEvent.ChangeDarkModeEnabled
import com.saurabhsandav.core.ui.settings.model.SettingsEvent.ChangeDefaultTimeframe
import com.saurabhsandav.core.ui.settings.model.SettingsEvent.ChangeDensityFraction
import com.saurabhsandav.core.ui.settings.model.SettingsEvent.ChangeLandingScreen
import com.saurabhsandav.core.ui.settings.model.SettingsState.Category
import com.saurabhsandav.core.ui.settings.ui.About
import com.saurabhsandav.core.ui.settings.ui.LayoutPreferences
import com.saurabhsandav.core.ui.settings.ui.PreferenceCategoryItem
import com.saurabhsandav.core.ui.settings.ui.TradingPreferences

@Composable
internal fun SettingsWindow(onCloseRequest: () -> Unit) {

    val scope = rememberCoroutineScope()
    val appGraph = LocalAppGraph.current
    val graph = remember { appGraph.settingsGraphFactory.create() }
    val presenter = remember { graph.presenterFactory.create(scope) }
    val state by presenter.state.collectAsState()

    AppWindow(
        title = "Settings",
        onCloseRequest = onCloseRequest,
    ) {

        SettingsScreen(
            layoutPane = {

                LayoutPreferences(
                    darkModeEnabled = state.darkModeEnabled,
                    onDarkThemeEnabledChange = { state.eventSink(ChangeDarkModeEnabled(it)) },
                    landingScreen = state.landingScreen,
                    onLandingScreenChange = { state.eventSink(ChangeLandingScreen(it)) },
                    densityFraction = state.densityFraction,
                    onDensityFractionChange = { state.eventSink(ChangeDensityFraction(it)) },
                )
            },
            tradingPane = {

                TradingPreferences(
                    defaultTimeframe = state.defaultTimeframe,
                    onDefaultTimeframeChange = { state.eventSink(ChangeDefaultTimeframe(it)) },
                )
            },
            backupPane = {

                BackupPreferencesPane(
                    settingsGraph = graph,
                )
            },
            aboutPane = { About() },
        )
    }
}

@Composable
internal fun SettingsScreen(
    layoutPane: @Composable ColumnScope.() -> Unit,
    tradingPane: @Composable ColumnScope.() -> Unit,
    backupPane: @Composable ColumnScope.() -> Unit,
    aboutPane: @Composable ColumnScope.() -> Unit,
) {

    var selectedCategory by state<Category?> { null }

    AnimatedContent(selectedCategory) { iSelectedCategory ->

        when (iSelectedCategory) {
            null -> {

                PreferenceCategories(
                    onLayoutClick = { selectedCategory = Category.Layout },
                    onTradingClick = { selectedCategory = Category.Trading },
                    onBackupClick = { selectedCategory = Category.Backup },
                    onAboutClick = { selectedCategory = Category.About },
                )
            }

            else -> {

                SelectedCategoryContent(
                    selectedCategory = iSelectedCategory,
                    onBackToSettings = { selectedCategory = null },
                    layoutPane = layoutPane,
                    tradingPane = tradingPane,
                    backupPane = backupPane,
                    aboutPane = aboutPane,
                )
            }
        }
    }
}

@Composable
private fun PreferenceCategories(
    onLayoutClick: () -> Unit,
    onTradingClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAboutClick: () -> Unit,
) {

    val scrollState = rememberScrollState()

    BoxWithScrollbar(
        scrollbarAdapter = rememberScrollbarAdapter(scrollState),
    ) {

        Column(
            modifier = Modifier.verticalScroll(scrollState),
        ) {

            PreferenceCategoryItem(
                onClick = onLayoutClick,
                headlineContent = { Text("Layout") },
            )

            HorizontalDivider()

            PreferenceCategoryItem(
                onClick = onTradingClick,
                headlineContent = { Text("Trading") },
            )

            HorizontalDivider()

            PreferenceCategoryItem(
                onClick = onBackupClick,
                headlineContent = { Text("Backup") },
            )

            HorizontalDivider()

            PreferenceCategoryItem(
                onClick = onAboutClick,
                headlineContent = { Text("About") },
            )
        }
    }
}

@Composable
private fun SelectedCategoryContent(
    selectedCategory: Category,
    onBackToSettings: () -> Unit,
    layoutPane: @Composable ColumnScope.() -> Unit,
    tradingPane: @Composable ColumnScope.() -> Unit,
    backupPane: @Composable ColumnScope.() -> Unit,
    aboutPane: @Composable ColumnScope.() -> Unit,
) {

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text(
                        text = when (selectedCategory) {
                            Category.Layout -> "Layout"
                            Category.Trading -> "Trading"
                            Category.Backup -> "Backup"
                            Category.About -> "About"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToSettings) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Settings")
                    }
                },
            )
        },
    ) { paddingValues ->

        val scrollState = rememberScrollState()

        BoxWithScrollbar(
            scrollbarAdapter = rememberScrollbarAdapter(scrollState),
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState),
            ) {

                when (selectedCategory) {
                    Category.Layout -> layoutPane()
                    Category.Trading -> tradingPane()
                    Category.Backup -> backupPane()
                    Category.About -> aboutPane()
                }
            }
        }
    }
}
