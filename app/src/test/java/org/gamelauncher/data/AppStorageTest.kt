package org.gamelauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppStorageTest {
    @Test
    fun usesPreferredWhenItCanBeCreated() {
        val tmp = createTempDir()
        val preferred = File(tmp, "ce")
        val fallback = File(tmp, "de")
        val result = pickWritableFilesDir(preferred, fallback)
        assertEquals(preferred.canonicalFile, result.canonicalFile)
        assertTrue(result.isDirectory)
    }

    @Test
    fun fallsBackWhenPreferredCannotBeCreated() {
        val tmp = createTempDir()
        val blocker = File(tmp, "not-a-dir").apply { writeText("x") }
        val preferred = File(blocker, "child")
        val fallback = File(tmp, "de")
        val result = pickWritableFilesDir(preferred, fallback)
        assertEquals(fallback.canonicalFile, result.canonicalFile)
        assertTrue(result.isDirectory)
    }
}
