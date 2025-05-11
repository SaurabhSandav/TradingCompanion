package com.saurabhsandav.core.backup.service

import kotlinx.serialization.Serializable
import java.nio.file.Path

interface BackupService {

    val id: Id

    val name: String

    fun newInstance(): Instance

    interface Instance {

        suspend fun saveBackup(archivePath: Path)
    }

    @Serializable
    @JvmInline
    value class Id(
        val value: String,
    )
}
