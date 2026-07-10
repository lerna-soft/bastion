package com.bastion.app.data

/**
 * HIM-019 — Lógica pura (sin Android, sin IO) para resolver cadenas de jump hosts (ProxyJump).
 * Separada de la UI y del repositorio para poder testearla directamente y para que ambos usen
 * exactamente la misma resolución (evita divergencias entre lo que muestra el selector y lo que
 * realmente conecta).
 */
object JumpHostChain {

    /**
     * Orden de conexión para llegar a [targetId]: sigue `jumpHostId` de cada host y devuelve los
     * ids ordenados del PRIMER salto (alcanzable directo desde el dispositivo) al DESTINO final
     * (último). Conexión directa → lista de un solo id.
     *
     * Robusto: si un `jumpHostId` apunta a un host inexistente (borrado), la cadena se corta ahí
     * en vez de fallar. Si hay un ciclo, corta al reencontrar un id ya visitado (nunca cuelga).
     * Si [targetId] no existe en [all], devuelve lista vacía.
     */
    fun resolveChainIds(all: List<Host>, targetId: Long): List<Long> {
        val byId = all.associateBy { it.id }
        val chain = ArrayDeque<Long>()
        val visited = mutableSetOf<Long>()
        var cur: Long? = targetId
        while (cur != null) {
            if (!visited.add(cur)) break            // ciclo → corta
            val host = byId[cur] ?: break           // id colgante → corta
            chain.addFirst(host.id)
            cur = host.jumpHostId
        }
        return chain.toList()
    }

    /**
     * Hosts que pueden usarse como jump host del host [selfId] sin crear un ciclo.
     * Excluye al propio host y a cualquier candidato cuya cadena de saltos ya pase por [selfId]
     * (elegirlo cerraría un bucle A→B→A que colgaría la resolución). Para un host nuevo
     * ([selfId] == null) todos los hosts son válidos.
     */
    fun candidates(all: List<Host>, selfId: Long?): List<Host> {
        if (all.isEmpty()) return emptyList()
        val byId = all.associateBy { it.id }
        fun chainReaches(startId: Long?, targetId: Long): Boolean {
            val visited = mutableSetOf<Long>()
            var cur = startId
            while (cur != null) {
                if (cur == targetId) return true
                if (!visited.add(cur)) return false // ciclo preexistente, corta
                cur = byId[cur]?.jumpHostId
            }
            return false
        }
        return all.filter { candidate ->
            candidate.id != selfId &&
                (selfId == null || !chainReaches(candidate.jumpHostId, selfId))
        }
    }
}
