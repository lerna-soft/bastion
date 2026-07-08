package com.bastion.app.logging

/**
 * Fixed-capacity ring buffer of recent log entries (HIM-009). Pure Kotlin — no Android deps — so
 * it is unit-testable. When full, the oldest entry is dropped.
 */
class LogRingBuffer(private val max: Int = 200) {
    private val items = ArrayDeque<RemoteLogger.RecentEntry>()

    @Synchronized
    fun add(entry: RemoteLogger.RecentEntry) {
        items.addLast(entry)
        while (items.size > max) items.removeFirst()
    }

    /** Snapshot, most-recent first. */
    @Synchronized
    fun snapshotNewestFirst(): List<RemoteLogger.RecentEntry> = items.toList().asReversed()

    @Synchronized
    fun clear() = items.clear()

    @Synchronized
    fun size(): Int = items.size
}
