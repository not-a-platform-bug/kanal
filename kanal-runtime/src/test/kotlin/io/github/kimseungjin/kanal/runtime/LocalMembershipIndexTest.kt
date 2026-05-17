package io.github.kimseungjin.kanal.runtime

import io.github.kimseungjin.kanal.core.ChannelAddress
import io.github.kimseungjin.kanal.core.ChannelPattern
import io.github.kimseungjin.kanal.core.SessionDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalMembershipIndexTest {
    @Test
    fun `indexes memberships by channel and session`() {
        val metrics = RuntimeMetrics()
        val index = LocalMembershipIndex(metrics)
        val session = SessionDescriptor(id = "s1", userId = "u1")
        val address = ChannelAddress(ChannelPattern("chat/{roomId}"), mapOf("roomId" to "alpha"))

        assertTrue(index.join(session, address))
        assertFalse(index.join(session, address))

        assertEquals(setOf("s1"), index.sessions(address))
        assertEquals(setOf("s1"), index.broadcastTargets(address))
        assertEquals(setOf(address), index.channels("s1"))
        assertEquals(1, index.activeMemberships())
        assertEquals(1, metrics.snapshot().activeMemberships)
        assertEquals(1, metrics.snapshot().maxObservedBroadcastFanOut)
    }

    @Test
    fun `removes memberships from both indexes`() {
        val metrics = RuntimeMetrics()
        val index = LocalMembershipIndex(metrics)
        val session = SessionDescriptor(id = "s1")
        val address = ChannelAddress(ChannelPattern("chat/{roomId}"), mapOf("roomId" to "alpha"))

        index.join(session, address)

        assertTrue(index.leave("s1", address))
        assertFalse(index.leave("s1", address))
        assertEquals(emptySet(), index.sessions(address))
        assertEquals(emptySet(), index.channels("s1"))
        assertEquals(0, metrics.snapshot().activeMemberships)
    }

    @Test
    fun `removes all memberships for a closed session`() {
        val index = LocalMembershipIndex()
        val session = SessionDescriptor(id = "s1")
        val alpha = ChannelAddress(ChannelPattern("chat/{roomId}"), mapOf("roomId" to "alpha"))
        val beta = ChannelAddress(ChannelPattern("chat/{roomId}"), mapOf("roomId" to "beta"))

        index.join(session, alpha)
        index.join(session, beta)

        assertEquals(setOf(alpha, beta), index.removeSession("s1"))
        assertEquals(emptySet(), index.sessions(alpha))
        assertEquals(emptySet(), index.sessions(beta))
    }
}
