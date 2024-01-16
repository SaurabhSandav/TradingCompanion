package com.saurabhsandav.core.ui.tradesfiltersheet.ui

import androidx.compose.runtime.Composable
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.Notes

@Composable
internal fun NotesFilterItem(
    notes: Notes,
    onNotesChange: (Notes) -> Unit,
) {

    TradeFilterChipGroup(
        title = "Notes",
        expandInitially = notes != Notes.All,
    ) {

        TradeFilterChip(
            label = "All",
            selected = notes == Notes.All,
            onClick = { onNotesChange(Notes.All) },
        )

        TradeFilterChip(
            label = "Has Notes",
            selected = notes == Notes.HasNotes,
            onClick = { onNotesChange(Notes.HasNotes) },
        )

        TradeFilterChip(
            label = "No Notes",
            selected = notes == Notes.NoNotes,
            onClick = { onNotesChange(Notes.NoNotes) },
        )
    }
}
