package com.bastion.app

import com.bastion.app.data.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `default settings have expected values`() {
        val s = AppSettings()
        assertEquals("BASTION-PRIME-01", s.serverName)
        assertEquals("EST (Eastern Standard Time)", s.timezone)
        assertEquals("English (United States)", s.language)
        assertEquals(14f, s.fontSize, 0.001f)
        assertTrue(s.twoFactorEnabled)
        assertEquals("30 Minutes", s.sessionTimeout)
        assertEquals("", s.webhookUrl)
        assertEquals("DARK", s.colorMode)
    }

    @Test
    fun `settings can be updated via copy`() {
        val s = AppSettings().copy(serverName = "PROD-01", fontSize = 16f)
        assertEquals("PROD-01", s.serverName)
        assertEquals(16f, s.fontSize, 0.001f)
    }
}
