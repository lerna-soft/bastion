package com.bastion.app.ui

data class ValidationResult(
    val isValid: Boolean,
    val hostnameError: Boolean = false,
    val usernameError: Boolean = false,
    val portError: Boolean = false
)

object HostValidator {
    fun validate(hostname: String, username: String, port: String): ValidationResult {
        var hostnameErr = false
        var usernameErr = false
        var portErr = false

        if (hostname.isBlank()) hostnameErr = true
        if (username.isBlank()) usernameErr = true
        val portNum = port.toIntOrNull()
        if (portNum == null || portNum < 1 || portNum > 65535) portErr = true

        return ValidationResult(
            isValid = !hostnameErr && !usernameErr && !portErr,
            hostnameError = hostnameErr,
            usernameError = usernameErr,
            portError = portErr
        )
    }

    fun countActive(keys: List<Any>, isActiveSelector: (Any) -> Boolean): Int {
        return keys.count { isActiveSelector(it) }
    }
}
