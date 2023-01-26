package ui.stockchart

import androidx.compose.runtime.*

class StockChartTabsState(
    private val onNew: (Int) -> Unit,
    private val onSelect: (Int) -> Unit,
    private val onClose: (Int) -> Unit,
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

        selectedTabIndex = tabs.lastIndex

        onNew(id)
    }

    fun selectTab(id: Int) {

        selectedTabIndex = tabs.indexOfFirst { it.id == id }

        onSelect(id)
    }

    fun closeTab(id: Int) {

        if (tabs.size == 1) return

        tabs.remove(tabs.first { it.id == id })

        onClose(id)
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

    fun setTitle(id: Int, title: String) {

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
