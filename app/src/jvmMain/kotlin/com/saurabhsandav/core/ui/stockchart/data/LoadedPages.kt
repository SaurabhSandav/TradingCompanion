package com.saurabhsandav.core.ui.stockchart.data

import kotlinx.datetime.Instant

internal class LoadedPages {

    private val _pages = mutableListOf<ClosedRange<Instant>>()
    val pages: List<ClosedRange<Instant>>
        get() = _pages

    val start: Instant
        get() = pages.first().start

    val endInclusive: Instant
        get() = pages.last().endInclusive

    val interval: ClosedRange<Instant>
        get() = start..endInclusive

    fun addBefore(range: ClosedRange<Instant>) {
        _pages.add(0, range)
    }

    fun addAfter(range: ClosedRange<Instant>) {
        _pages.add(range)
    }

    fun dropBefore() {
        _pages.removeFirst()
    }

    fun dropAfter() {
        _pages.removeLast()
    }

    fun clear() = _pages.clear()

    fun isEmpty() = _pages.isEmpty()
}
