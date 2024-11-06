package com.saurabhsandav.core.utils

import app.cash.turbine.test
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZipUtilsTest {

    @Test
    fun `Zip and Unzip`() = runTest {

        val fakeFileSystem = Jimfs.newFileSystem(Configuration.unix())
        val outFile = fakeFileSystem.getPath("/out.zip")

        val file1Text = "This if file 1."
        val file2Text = "This if file 2."

        val file1 = fakeFileSystem.getPath("file1")
        val file2 = fakeFileSystem.getPath("file2")
        val dir1 = fakeFileSystem.getPath("dir1")
        val dir2 = fakeFileSystem.getPath("dir2")

        val paths = fakeFileSystem.getPath("/in").let { inDir ->

            assertTrue { inDir.notExists() }

            // Create in dir
            inDir.createDirectories()

            assertTrue { inDir.exists() }

            val inFile1 = inDir.resolve(file1).apply {
                createFile()
                writeText(file1Text)
            }

            val inDir1 = inDir.resolve(dir1).createDirectories()

            val inFile2 = inDir1.resolve(file2).apply {
                createFile()
                writeText(file2Text)
            }

            val inDir2 = inDir.resolve(dir2).createDirectories()

            assertTrue { inFile1.exists() }
            assertEquals(file1Text, inFile1.readText())
            assertTrue { inDir1.exists() }
            assertTrue { inFile2.exists() }
            assertTrue { inDir2.exists() }
            assertEquals(file2Text, inFile2.readText())
            assertTrue { outFile.notExists() }

            listOf(inFile1, inDir1, inDir2)
        }

        assertTrue { outFile.notExists() }

        // Create Zip
        ZipUtils.zip(
            paths = paths,
            outPath = outFile,
        )

        assertTrue { outFile.exists() }

        val outDir = fakeFileSystem.getPath("/out").apply {

            assertTrue { notExists() }

            // Create out dir
            createDirectories()

            assertTrue { exists() }
        }

        // Extract Zip
        ZipUtils.unzip(
            zipPath = outFile,
            outDir = outDir,
        )

        val outFile1 = outDir.resolve(file1)
        val outDir1 = outDir.resolve(dir1)
        val outFile2 = outDir1.resolve(file2)
        val outDir2 = outDir.resolve(dir2)

        assertTrue { outFile1.exists() }
        assertEquals(file1Text, outFile1.readText())
        assertTrue { outDir1.exists() }
        assertTrue { outFile2.exists() }
        assertTrue { outDir2.exists() }
        assertEquals(file2Text, outFile2.readText())
    }

    @Test
    fun `Zip progress`() = runTest {

        val fakeFileSystem = Jimfs.newFileSystem(Configuration.unix())
        val outFile = fakeFileSystem.getPath("/out.zip")

        val file1Text = "This if file 1."
        val file2Text = "This if file 2."

        val file1 = fakeFileSystem.getPath("file1")
        val file2 = fakeFileSystem.getPath("file2")
        val dir1 = fakeFileSystem.getPath("dir1")
        val dir2 = fakeFileSystem.getPath("dir2")

        val paths = fakeFileSystem.getPath("/in").let { inDir ->

            // Create in dir
            inDir.createDirectories()

            val inFile1 = inDir.resolve(file1).apply {
                createFile()
                writeText(file1Text)
            }

            val inDir1 = inDir.resolve(dir1).createDirectories()

            inDir1.resolve(file2).apply {
                createFile()
                writeText(file2Text)
            }

            val inDir2 = inDir.resolve(dir2).createDirectories()

            listOf(inFile1, inDir1, inDir2)
        }

        val channel = Channel<ZipProgress>(Channel.BUFFERED)

        // Create Zip
        ZipUtils.zip(
            paths = paths,
            outPath = outFile,
            onProgress = channel::trySend,
        )

        channel.consumeAsFlow().test {
            assertEquals(ZipProgress(0, 15, null), awaitItem())
            assertEquals(ZipProgress(0, 30, null), awaitItem())
            assertEquals(ZipProgress(0, 30, PathZipProgress("/in/file1", 0, 15)), awaitItem())
            assertEquals(ZipProgress(15, 30, PathZipProgress("/in/file1", 15, 15)), awaitItem())
            assertEquals(ZipProgress(15, 30, PathZipProgress("/in/dir1", 0, 0)), awaitItem())
            assertEquals(ZipProgress(15, 30, PathZipProgress("/in/dir1/file2", 0, 15)), awaitItem())
            assertEquals(ZipProgress(30, 30, PathZipProgress("/in/dir1/file2", 15, 15)), awaitItem())
            assertEquals(ZipProgress(30, 30, PathZipProgress("/in/dir2", 0, 0)), awaitItem())
            assertEquals(ZipProgress(30, 30, null), awaitItem())
        }
    }

    @Test
    fun `Unzip progress`() = runTest {

        val fakeFileSystem = Jimfs.newFileSystem(Configuration.unix())
        val outFile = fakeFileSystem.getPath("/out.zip")

        val file1Text = "This if file 1."
        val file2Text = "This if file 2."

        val file1 = fakeFileSystem.getPath("file1")
        val file2 = fakeFileSystem.getPath("file2")
        val dir1 = fakeFileSystem.getPath("dir1")
        val dir2 = fakeFileSystem.getPath("dir2")

        val paths = fakeFileSystem.getPath("/in").let { inDir ->

            // Create in dir
            inDir.createDirectories()

            val inFile1 = inDir.resolve(file1).apply {
                createFile()
                writeText(file1Text)
            }

            val inDir1 = inDir.resolve(dir1).createDirectories()

            inDir1.resolve(file2).apply {
                createFile()
                writeText(file2Text)
            }

            val inDir2 = inDir.resolve(dir2).createDirectories()

            listOf(inFile1, inDir1, inDir2)
        }

        // Create Zip
        ZipUtils.zip(
            paths = paths,
            outPath = outFile,
        )

        // Create out dir
        val outDir = fakeFileSystem.getPath("/out").createDirectories()
        val channel = Channel<ZipProgress>(Channel.BUFFERED)

        // Extract Zip
        ZipUtils.unzip(
            zipPath = outFile,
            outDir = outDir,
            onProgress = channel::trySend,
        )

        channel.consumeAsFlow().test {
            assertEquals(ZipProgress(0, 15, null), awaitItem())
            assertEquals(ZipProgress(0, 30, null), awaitItem())
            assertEquals(ZipProgress(0, 30, PathZipProgress("file1", 0, 15)), awaitItem())
            assertEquals(ZipProgress(15, 30, PathZipProgress("file1", 15, 15)), awaitItem())
            assertEquals(ZipProgress(15, 30, PathZipProgress("dir1/", 0, 0)), awaitItem())
            assertEquals(ZipProgress(15, 30, PathZipProgress("dir1/file2", 0, 15)), awaitItem())
            assertEquals(ZipProgress(30, 30, PathZipProgress("dir1/file2", 15, 15)), awaitItem())
            assertEquals(ZipProgress(30, 30, PathZipProgress("dir2/", 0, 0)), awaitItem())
            assertEquals(ZipProgress(30, 30, null), awaitItem())
        }
    }
}
