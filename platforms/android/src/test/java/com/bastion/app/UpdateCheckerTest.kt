package com.bastion.app

import com.bastion.app.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure logic extracted from UpdateChecker (HIM-007).
 * These run under plain JUnit — the IO/Context-bound parts (network, FileProvider,
 * startActivity) are intentionally not exercised here.
 */
class UpdateCheckerTest {

    private val validResponse = """
        {
          "update": true,
          "versionName": "1.1.10",
          "versionCode": 22,
          "downloadUrl": "http://192.168.0.100:8765/apk-share/bastion-v1.1.10.apk",
          "fileName": "bastion-v1.1.10.apk",
          "timestamp": "20260708-1000",
          "fileSize": 23609861,
          "changelog": "fix update flow"
        }
    """.trimIndent()

    // --- T1: parseUpdateResponse -------------------------------------------------

    @Test
    fun `parseUpdateResponse returns info when server code is newer`() {
        val info = UpdateChecker.parseUpdateResponse(validResponse, localCode = 21)
        assertEquals("1.1.10", info?.versionName)
        assertEquals(22, info?.versionCode)
        assertEquals("bastion-v1.1.10.apk", info?.fileName)
        assertEquals(23609861L, info?.fileSize)
    }

    @Test
    fun `parseUpdateResponse returns null when server code equals local`() {
        assertNull(UpdateChecker.parseUpdateResponse(validResponse, localCode = 22))
    }

    @Test
    fun `parseUpdateResponse returns null when server code is older`() {
        assertNull(UpdateChecker.parseUpdateResponse(validResponse, localCode = 99))
    }

    @Test
    fun `parseUpdateResponse returns null when update flag is false`() {
        val body = """{"update": false, "versionCode": 22}"""
        assertNull(UpdateChecker.parseUpdateResponse(body, localCode = 1))
    }

    // --- T2: isValidDownload -----------------------------------------------------

    @Test
    fun `isValidDownload true when size matches expected`() {
        assertTrue(UpdateChecker.isValidDownload(actualLength = 100L, expectedSize = 100L))
    }

    @Test
    fun `isValidDownload false when size does not match expected`() {
        assertFalse(UpdateChecker.isValidDownload(actualLength = 80L, expectedSize = 100L))
    }

    @Test
    fun `isValidDownload false when file is empty`() {
        assertFalse(UpdateChecker.isValidDownload(actualLength = 0L, expectedSize = 100L))
    }

    @Test
    fun `isValidDownload true when expected size unknown but file non-empty`() {
        assertTrue(UpdateChecker.isValidDownload(actualLength = 50L, expectedSize = 0L))
    }

    // --- T3: isInstallPermissionGranted -----------------------------------------

    @Test
    fun `install permission granted on pre-Oreo regardless of flag`() {
        assertTrue(UpdateChecker.isInstallPermissionGranted(sdkInt = 25, canRequestPackageInstalls = false))
    }

    @Test
    fun `install permission granted on Oreo plus when flag is true`() {
        assertTrue(UpdateChecker.isInstallPermissionGranted(sdkInt = 26, canRequestPackageInstalls = true))
    }

    @Test
    fun `install permission denied on Oreo plus when flag is false`() {
        assertFalse(UpdateChecker.isInstallPermissionGranted(sdkInt = 34, canRequestPackageInstalls = false))
    }
}
