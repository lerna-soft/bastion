package com.bastion.app.util

import com.bastion.app.logging.RemoteLogger

/**
 * Runs [block], catching and logging any exception so it can never crash the app (HIM-009,
 * requisito duro: "todos los errores deben ser capturados; la app nunca debe cerrarse").
 * Use it to wrap UI callbacks and coroutine bodies that run real logic.
 */
inline fun safe(tag: String = "safe", block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        RemoteLogger.e(tag, "safe caught: ${e.message}", e)
    }
}
