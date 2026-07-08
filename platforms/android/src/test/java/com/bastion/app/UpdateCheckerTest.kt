package com.bastion.app

import com.bastion.app.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure logic extracted from UpdateChecker (HIM-007, HIM-018).
 * These run under plain JUnit — the IO/Context-bound parts (network, FileProvider,
 * startActivity) are intentionally not exercised here.
 */
class UpdateCheckerTest {

    private val validGithubRelease = """
        {
          "tag_name": "v1.1.25",
          "body": "fix update flow",
          "published_at": "2026-07-08T20:00:00Z",
          "assets": [
            {
              "name": "bastion-android-v1.1.25.apk",
              "browser_download_url": "https://github.com/lerna-admin/bastion/releases/download/v1.1.25/bastion-android-v1.1.25.apk",
              "size": 23609861
            }
          ]
        }
    """.trimIndent()

    // --- T1: parseGithubRelease ---------------------------------------------------

    @Test
    fun `parseGithubRelease returns info when tag is newer`() {
        val info = UpdateChecker.parseGithubRelease(validGithubRelease, localVersion = "1.1.24")
        assertEquals("1.1.25", info?.versionName)
        assertEquals("bastion-android-v1.1.25.apk", info?.fileName)
        assertEquals(23609861L, info?.fileSize)
        assertEquals(
            "https://github.com/lerna-admin/bastion/releases/download/v1.1.25/bastion-android-v1.1.25.apk",
            info?.downloadUrl
        )
    }

    @Test
    fun `parseGithubRelease returns null when tag equals local`() {
        assertNull(UpdateChecker.parseGithubRelease(validGithubRelease, localVersion = "1.1.25"))
    }

    @Test
    fun `parseGithubRelease returns null when tag is older`() {
        assertNull(UpdateChecker.parseGithubRelease(validGithubRelease, localVersion = "9.9.9"))
    }

    @Test
    fun `parseGithubRelease returns null when no android asset present`() {
        val body = """{"tag_name": "v1.1.25", "assets": [{"name": "bastion-desktop-linux.deb", "browser_download_url": "x", "size": 1}]}"""
        assertNull(UpdateChecker.parseGithubRelease(body, localVersion = "1.1.24"))
    }

    // --- isNewerVersion (semver) --------------------------------------------------

    @Test
    fun `isNewerVersion true when patch is greater`() {
        assertTrue(UpdateChecker.isNewerVersion("v1.1.25", "1.1.24"))
    }

    @Test
    fun `isNewerVersion false when equal`() {
        assertFalse(UpdateChecker.isNewerVersion("v1.1.24", "1.1.24"))
    }

    @Test
    fun `isNewerVersion false when older`() {
        assertFalse(UpdateChecker.isNewerVersion("v1.0.0", "1.1.24"))
    }

    @Test
    fun `isNewerVersion true when major is greater`() {
        assertTrue(UpdateChecker.isNewerVersion("2.0.0", "1.9.9"))
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
