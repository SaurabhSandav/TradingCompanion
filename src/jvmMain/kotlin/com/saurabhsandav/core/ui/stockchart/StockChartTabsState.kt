package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.*

class StockChartTabsState(
    private val onNew: (
        tabId: Int,
        prevTabId: Int,
        updateTitle: (String) -> Unit,
    ) -> Unit,
    private val onSelect: (tabId: Int) -> Unit,
    private val onClose: (tabId: Int) -> Unit,
) {

    private var nextId = 0

    val tabs = mutableStateListOf<TabInfo>()
    var selectedTabIndex by mutableStateOf(0)
        private set

    init {
        newTab()
    }

    fun newTab() {

        val id = nextId++

        tabs.add(TabInfo(id, ""))

        val prevTabId = selectedTabIndex

        onNew(id, prevTabId) { setTitle(id, it) }

        selectTab(id)
    }

    fun selectTab(id: Int) {

        selectedTabIndex = tabs.indexOfFirst { it.id == id }

        onSelect(id)
    }

    fun closeTab(id: Int) {

        if (tabs.size <= 1) return

        val tabIndex = tabs.indexOfFirst { it.id == id }

        if (tabIndex == selectedTabIndex) {

            // Try selecting next tab, or if current tab is last, select previous tab
            val nextSelectedTab = when {
                tabIndex != tabs.lastIndex -> tabs[tabIndex + 1]
                else -> tabs[tabIndex - 1]
            }

            // Close tab
            tabs.removeAt(tabIndex)

            // Select another tab
            selectTab(nextSelectedTab.id)
        } else {

            val currentTab = tabs[selectedTabIndex]

            // Close tab
            tabs.remove(tabs.first { it.id == id })

            // Update selection to reflect list index changes
            selectedTabIndex = tabs.indexOf(currentTab)
        }

        onClose(id)
    }

    fun closeCurrentTab() {
        closeTab(tabs[selectedTabIndex].id)
    }

    fun selectNextTab() {
        val nextSelectionIndex = if (selectedTabIndex == tabs.lastIndex) 0 else selectedTabIndex + 1
        selectTab(tabs[nextSelectionIndex].id)
    }

    fun selectPreviousTab() {
        val previousSelectionIndex = if (selectedTabIndex == 0) tabs.lastIndex else selectedTabIndex - 1
        selectTab(tabs[previousSelectionIndex].id)
    }

    fun moveTabBackward() {

        if (selectedTabIndex == 0) return

        val tab = tabs.removeAt(selectedTabIndex)

        tabs.add(--selectedTabIndex, tab)
    }

    fun moveTabForward() {

        if (selectedTabIndex == tabs.lastIndex) return

        val tab = tabs.removeAt(selectedTabIndex)

        tabs.add(++selectedTabIndex, tab)
    }

    private fun setTitle(id: Int, title: String) {

        val index = tabs.indexOfFirst { it.id == id }

        tabs.removeAt(index)
        tabs.add(index, TabInfo(id, title))
    }

    @Immutable
    class TabInfo(
        val id: Int,
        val title: String,
    )
}
