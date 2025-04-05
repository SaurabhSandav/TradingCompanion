package com.saurabhsandav.core.ui.stockchart.data

import kotlinx.datetime.Instant

internal class LoadedPages {

    private val pages = mutableListOf<ClosedRange<Instant>>()

    val start: Instant
        get() = nonEmptyPages().first().start

    val endInclusive: Instant
        get() = nonEmptyPages().last().endInclusive

    val interval: ClosedRange<Instant>
        get() = start..endInclusive

    private fun nonEmptyPages(): List<ClosedRange<Instant>> {
        check(pages.isNotEmpty()) { "No pages loaded" }
        return pages
    }

    fun addBefore(range: ClosedRange<Instant>) {

        if (pages.isNotEmpty()) {
            require(range.endInclusive < start) { "New page interval should end before current interval" }
        }

        pages.add(0, range)
    }

    fun addAfter(range: ClosedRange<Instant>) {

        if (pages.isNotEmpty()) {
            require(endInclusive < range.endInclusive) { "New page interval should start after current interval" }
        }

        pages.add(range)
    }

    fun dropBefore() {
        pages.removeFirst()
    }

    fun dropAfter() {
        pages.removeLast()
    }

    fun clear() = pages.clear()

    fun isEmpty() = pages.isEmpty()

    fun replaceAllWith(other: LoadedPages) {
        pages.clear()
        pages.addAll(other.pages)
    }
}
