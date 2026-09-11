package com.dsh.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlHelperTest {

    @Test
    fun testNormalizeDefaultUrl() {
        assertEquals("http://192.168.0.102:3080/", UrlHelper.normalize(null))
        assertEquals("http://192.168.0.102:3080/", UrlHelper.normalize(""))
        assertEquals("http://192.168.0.102:3080/", UrlHelper.normalize("   "))
    }

    @Test
    fun testNormalizePrependsHttp() {
        assertEquals("http://192.168.0.102:3080/", UrlHelper.normalize("192.168.0.102:3080"))
        assertEquals("http://localhost:3080/", UrlHelper.normalize("localhost:3080"))
    }

    @Test
    fun testNormalizePreservesHttps() {
        assertEquals("https://dsh.example.com/", UrlHelper.normalize("https://dsh.example.com"))
    }

    @Test
    fun testNormalizePreservesPath() {
        assertEquals("http://192.168.0.102:3080/dashboard", UrlHelper.normalize("192.168.0.102:3080/dashboard"))
    }

    @Test
    fun testIsValid() {
        assertTrue(UrlHelper.isValid("http://192.168.0.102:3080/"))
        assertTrue(UrlHelper.isValid("192.168.0.102:3080"))
        assertTrue(UrlHelper.isValid("https://example.com"))
        assertFalse(UrlHelper.isValid(""))
        assertFalse(UrlHelper.isValid("   "))
        assertFalse(UrlHelper.isValid("http://:3080"))
    }
}
