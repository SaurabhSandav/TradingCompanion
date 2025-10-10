package com.saurabhsandav.core

import com.saurabhsandav.core.utils.AppPaths
import com.saurabhsandav.core.utils.RealAppPaths
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.nio.file.FileSystem
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.random.Random

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, replaces = [RealAppPaths::class])
@Inject
class FakeAppPaths(
    private val fakeFileSystem: FileSystem,
) : AppPaths {

    override val appName: String = "TC"

    override val appDataPath: Path = fakeFileSystem.getPath("/data").also { it.createDirectories() }

    override fun createTempDirectory(prefix: String): Path {
        return fakeFileSystem.getPath("/${prefix}_${Random.nextLong()}").also { it.createDirectories() }
    }
}
