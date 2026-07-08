package com.bastion.app

import com.bastion.app.ui.HostValidator
import com.bastion.app.ui.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HostValidatorTest {

    @Test
    fun `valid hostname username and port returns isValid=true`() {
        val result = HostValidator.validate("10.0.0.5", "root", "22")
        assertTrue(result.isValid)
        assertFalse(result.hostnameError)
        assertFalse(result.usernameError)
        assertFalse(result.portError)
    }

    @Test
    fun `blank hostname returns hostnameError=true`() {
        val result = HostValidator.validate("", "root", "22")
        assertFalse(result.isValid)
        assertTrue(result.hostnameError)
    }

    @Test
    fun `blank username returns usernameError=true`() {
        val result = HostValidator.validate("10.0.0.5", "", "22")
        assertFalse(result.isValid)
        assertTrue(result.usernameError)
    }

    @Test
    fun `port out of range returns portError=true`() {
        val result = HostValidator.validate("10.0.0.5", "root", "0")
        assertFalse(result.isValid)
        assertTrue(result.portError)
    }

    @Test
    fun `non-numeric port returns portError=true`() {
        val result = HostValidator.validate("10.0.0.5", "root", "abc")
        assertFalse(result.isValid)
        assertTrue(result.portError)
    }

    @Test
    fun `port 65535 is valid`() {
        val result = HostValidator.validate("10.0.0.5", "root", "65535")
        assertTrue(result.isValid)
        assertFalse(result.portError)
    }

    @Test
    fun `empty hostname and username returns both errors`() {
        val result = HostValidator.validate("", "", "22")
        assertFalse(result.isValid)
        assertTrue(result.hostnameError)
        assertTrue(result.usernameError)
    }

    @Test
    fun `countActive returns correct count`() {
        data class FakeKey(val name: String, val active: Boolean)
        val keys = listOf(
            FakeKey("k1", true),
            FakeKey("k2", false),
            FakeKey("k3", true),
        )
        val count = HostValidator.countActive(keys) { (it as FakeKey).active }
        assertEquals(2, count)
    }

    @Test
    fun `countActive with empty list returns zero`() {
        val count = HostValidator.countActive(emptyList()) { true }
        assertEquals(0, count)
    }
}
