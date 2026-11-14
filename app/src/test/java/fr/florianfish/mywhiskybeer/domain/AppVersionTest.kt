package fr.florianfish.mywhiskybeer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun release() {
        val v = AppVersion.parse("1.4.0", isDebugBuild = false, commit = "abc1234")
        assertFalse(v.isDebug)
        assertEquals("1.4.0", v.release)
        assertNull(v.debugRun)
        assertEquals("Version officielle", v.description)
    }

    @Test
    fun ciDebug() {
        val v = AppVersion.parse("1.4.0-debug.57", isDebugBuild = true, commit = "abc1234")
        assertTrue(v.isDebug)
        assertEquals("1.4.0", v.release)
        assertEquals(57, v.debugRun)
        assertEquals("Build de test n° 57, compilé après la version 1.4.0", v.description)
    }

    @Test
    fun localDebug() {
        val v = AppVersion.parse("0.1.0-debug", isDebugBuild = true, commit = "local")
        assertEquals("0.1.0", v.release)
        assertNull(v.debugRun)
        assertTrue(v.description.startsWith("Build de test local"))
    }
}
