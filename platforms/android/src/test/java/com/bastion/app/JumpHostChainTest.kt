package com.bastion.app

import com.bastion.app.data.Host
import com.bastion.app.data.JumpHostChain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HIM-019 — Tests de la resolución pura de cadenas de jump hosts (ProxyJump).
 * Cubre: conexión directa, cadena lineal A→B→C, id colgante (host borrado), ciclos
 * (no debe colgar), y los candidatos válidos del selector (excluye self y ciclos).
 */
class JumpHostChainTest {

    private fun host(id: Long, jump: Long? = null) =
        Host(id = id, name = "h$id", hostname = "10.0.0.$id", username = "root", jumpHostId = jump)

    // ── resolveChainIds ────────────────────────────────────────────

    @Test
    fun `direct connection returns single target id`() {
        val all = listOf(host(1))
        assertEquals(listOf(1L), JumpHostChain.resolveChainIds(all, 1))
    }

    @Test
    fun `linear chain resolves first-hop to target order`() {
        // C(3)→B(2)→A(1);  A es el primer salto alcanzable directo, C el destino.
        val a = host(1)
        val b = host(2, jump = 1)
        val c = host(3, jump = 2)
        val all = listOf(c, b, a) // orden de entrada arbitrario a propósito
        assertEquals(listOf(1L, 2L, 3L), JumpHostChain.resolveChainIds(all, 3))
    }

    @Test
    fun `dangling jump id truncates chain instead of failing`() {
        // B apunta a un jump host 99 que ya no existe → la cadena se corta en B.
        val b = host(2, jump = 99)
        val all = listOf(b)
        assertEquals(listOf(2L), JumpHostChain.resolveChainIds(all, 2))
    }

    @Test
    fun `cycle terminates and does not loop forever`() {
        // A(1)→B(2)→A(1) ciclo. Debe terminar (no colgar) devolviendo la porción alcanzable.
        val a = host(1, jump = 2)
        val b = host(2, jump = 1)
        val all = listOf(a, b)
        val result = JumpHostChain.resolveChainIds(all, 1)
        assertTrue("debe terminar con la porción alcanzable", result.isNotEmpty())
        assertEquals(2, result.size)
        assertEquals(1L, result.last()) // el destino pedido queda al final
    }

    @Test
    fun `unknown target returns empty`() {
        val all = listOf(host(1))
        assertEquals(emptyList<Long>(), JumpHostChain.resolveChainIds(all, 42))
    }

    // ── candidates (selector de jump host) ─────────────────────────

    @Test
    fun `candidates for new host returns all hosts`() {
        val all = listOf(host(1), host(2))
        assertEquals(all, JumpHostChain.candidates(all, selfId = null))
    }

    @Test
    fun `candidates excludes self`() {
        val all = listOf(host(1), host(2))
        val result = JumpHostChain.candidates(all, selfId = 1)
        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `candidates excludes hosts whose chain reaches self`() {
        // Editando A(1). B(2)→A y C(3)→B→A ambos vuelven a A (crearían ciclo) → excluidos.
        // D(4) es independiente → válido.
        val a = host(1)
        val b = host(2, jump = 1)
        val c = host(3, jump = 2)
        val d = host(4)
        val all = listOf(a, b, c, d)
        val result = JumpHostChain.candidates(all, selfId = 1).map { it.id }
        assertEquals(listOf(4L), result)
    }

    @Test
    fun `candidates allows independent host as jump`() {
        // Editando C(3). A(1) es raíz independiente → válido como salto de C.
        val a = host(1)
        val c = host(3)
        val all = listOf(a, c)
        val result = JumpHostChain.candidates(all, selfId = 3).map { it.id }
        assertEquals(listOf(1L), result)
    }
}
