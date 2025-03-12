package com.saurabhsandav.core.backup

enum class BackupEvent {
    GeneratingArchive,
    SavingArchive,
    Finished,
}

enum class RestoreEvent {
    ExtractingArchive,
    ReplacingAppFiles,
    Finished,
}
