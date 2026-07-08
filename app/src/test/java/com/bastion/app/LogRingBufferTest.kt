package com.bastion.app

import com.bastion.app.logging.LogRingBuffer
import com.bastion.app.logging.RemoteLogger
import org.junit.Assert.assertEquals
import org.junit.Test

class LogRingBufferTest {

    private fun entry(i: Int) = RemoteLogger.RecentEntry(
        timeMillis = i.toLong(),
        level = "INFO",
        tag = "t",
        msg = "msg-$i"
    )

    @Test
    fun `keeps at most max entries and drops the oldest`() {
        val buf = LogRingBuffer(max = 200)
        for (i in 1..250) buf.add(entry(i))
        assertEquals(200, buf.size())
        val snapshot = buf.snapshotNewestFirst()
        // Newest first: msg-250 first, and the oldest surviving is msg-51 (250-200+1)
        assertEquals("msg-250", snapshot.first().msg)
        assertEquals("msg-51", snapshot.last().msg)
    }

    @Test
    fun `snapshot is newest first`() {
        val buf = LogRingBuffer(max = 10)
        buf.add(entry(1))
        buf.add(entry(2))
        buf.add(entry(3))
        val snapshot = buf.snapshotNewestFirst()
        assertEquals(listOf("msg-3", "msg-2", "msg-1"), snapshot.map { it.msg })
    }

    @Test
    fun `clear empties the buffer`() {
        val buf = LogRingBuffer(max = 10)
        buf.add(entry(1))
        buf.clear()
        assertEquals(0, buf.size())
    }

    @Test
    fun `under capacity keeps all entries`() {
        val buf = LogRingBuffer(max = 100)
        for (i in 1..5) buf.add(entry(i))
        assertEquals(5, buf.size())
    }
}
