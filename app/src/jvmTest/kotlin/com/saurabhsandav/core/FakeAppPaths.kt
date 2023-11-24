package com.saurabhsandav.core

import com.saurabhsandav.core.utils.AppPaths
import java.nio.file.FileSystem
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.random.Random

class FakeAppPaths(private val fakeFileSystem: FileSystem) : AppPaths {

    override val appName: String = "TC"

    override val appDataPath: Path = fakeFileSystem.getPath("/data").also { it.createDirectories() }

    override fun createTempDirectory(prefix: String): Path {
        return fakeFileSystem.getPath("/${prefix}_${Random.nextLong()}").also { it.createDirectories() }
    }
}
