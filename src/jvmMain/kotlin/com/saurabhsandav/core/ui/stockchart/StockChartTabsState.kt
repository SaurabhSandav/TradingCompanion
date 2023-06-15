package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.*

@Stable
class StockChartTabsState(
    private val onNew: (tabId: Int) -> Unit,
    private val onSelect: (tabId: Int) -> Unit,
    private val onClose: (tabId: Int) -> Unit,
    val title: (tabId: Int) -> String,
) {

    private var nextId = 0

    val tabIds = mutableStateListOf<Int>()

    var selectedTabIndex by mutableStateOf(0)
        private set

    init {
        newTab()
    }

    fun newTab() {

        val id = nextId++

        tabIds.add(id)

        onNew(id)

        selectTab(id)
    }

    fun selectTab(id: Int) {

        selectedTabIndex = tabIds.indexOf(id)

        onSelect(id)
    }

    fun closeTab(id: Int) {

        if (tabIds.size <= 1) return

        val tabIndex = tabIds.indexOf(id)

        if (tabIndex == selectedTabIndex) {

            // Try selecting next tab, or if current tab is last, select previous tab
            val nextSelectedTabId = when {
                tabIndex != tabIds.lastIndex -> tabIds[tabIndex + 1]
                else -> tabIds[tabIndex - 1]
            }

            // Close tab
            tabIds.remove(id)

            // Select another tab
            selectTab(nextSelectedTabId)
        } else {

            val currentTabId = tabIds[selectedTabIndex]

            // Close tab
            tabIds.remove(id)

            // Update selection to reflect list index changes
            selectedTabIndex = tabIds.indexOf(currentTabId)
        }

        onClose(id)
    }

    fun closeCurrentTab() {
        closeTab(tabIds[selectedTabIndex])
    }

    fun selectNextTab() {
        val nextSelectionIndex = if (selectedTabIndex == tabIds.lastIndex) 0 else selectedTabIndex + 1
        selectTab(tabIds[nextSelectionIndex])
    }

    fun selectPreviousTab() {
        val previousSelectionIndex = if (selectedTabIndex == 0) tabIds.lastIndex else selectedTabIndex - 1
        selectTab(tabIds[previousSelectionIndex])
    }

    fun moveTabBackward() {

        if (selectedTabIndex == 0) return

        val tabId = tabIds.removeAt(selectedTabIndex)

        tabIds.add(--selectedTabIndex, tabId)
    }

    fun moveTabForward() {

        if (selectedTabIndex == tabIds.lastIndex) return

        val tabId = tabIds.removeAt(selectedTabIndex)

        tabIds.add(++selectedTabIndex, tabId)
    }
}
