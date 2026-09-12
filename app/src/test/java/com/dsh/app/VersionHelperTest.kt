package com.dsh.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionHelperTest {

    @Test
    fun clean_removesPrefixesAndWhitespace() {
        assertEquals("1.0.0", VersionHelper.clean("v1.0.0"))
        assertEquals("1.0.0", VersionHelper.clean("V1.0.0"))
        assertEquals("1.0.0", VersionHelper.clean("  1.0.0  "))
        assertEquals("2.3.4", VersionHelper.clean("v2.3.4-debug"))
    }

    @Test
    fun isNewer_returnsTrue_whenRemoteHasHigherMajor() {
        assertTrue(VersionHelper.isNewer("2.0.0", "1.0.0"))
        assertTrue(VersionHelper.isNewer("v2.0.0", "v1.9.9"))
    }

    @Test
    fun isNewer_returnsTrue_whenRemoteHasHigherMinor() {
        assertTrue(VersionHelper.isNewer("1.1.0", "1.0.0"))
        assertTrue(VersionHelper.isNewer("v1.2.0", "v1.1.9"))
    }

    @Test
    fun isNewer_returnsTrue_whenRemoteHasHigherPatch() {
        assertTrue(VersionHelper.isNewer("1.0.1", "1.0.0"))
        assertTrue(VersionHelper.isNewer("v1.0.2", "1.0.1"))
    }

    @Test
    fun isNewer_returnsFalse_whenEqualOrLower() {
        assertFalse(VersionHelper.isNewer("1.0.0", "1.0.0"))
        assertFalse(VersionHelper.isNewer("v1.0.0", "1.0.0"))
        assertFalse(VersionHelper.isNewer("0.9.9", "1.0.0"))
        assertFalse(VersionHelper.isNewer("1.0.0", "1.0.1"))
        assertFalse(VersionHelper.isNewer("1.0.0", "1.1.0"))
    }

    @Test
    fun isNewer_handlesDifferentLengthComponents() {
        assertTrue(VersionHelper.isNewer("1.0.1", "1.0"))
        assertFalse(VersionHelper.isNewer("1.0", "1.0.1"))
        assertFalse(VersionHelper.isNewer("1.0.0", "1.0"))
    }

    @Test
    fun isNewer_handlesEmptyOrInvalidStringsSafely() {
        assertFalse(VersionHelper.isNewer("", "1.0.0"))
        assertFalse(VersionHelper.isNewer("invalid", "1.0.0"))
    }
}
