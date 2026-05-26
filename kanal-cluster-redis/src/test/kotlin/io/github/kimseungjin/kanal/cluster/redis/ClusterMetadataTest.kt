package io.github.kimseungjin.kanal.cluster.redis

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ClusterMetadataTest {
    @Test
    fun `models remote session membership presence and broadcast envelopes`() {
        val nodeId = ClusterNodeId("node-a")
        val connectedAt = Instant.parse("2026-05-26T00:00:00Z")

        val session =
            ClusterSessionDescriptor(
                sessionId = "s1",
                nodeId = nodeId,
                userId = "u1",
                connectedAt = connectedAt,
            )
        val membership =
            ClusterChannelMembership(
                channel = "chat/general",
                sessionId = "s1",
                nodeId = nodeId,
                joinedAt = connectedAt,
            )
        val presence =
            ClusterPresenceEntry(
                channel = "chat/general",
                key = "u1",
                nodeId = nodeId,
                sessionId = "s1",
                metadata = mapOf("displayName" to "Mina"),
                joinedAt = connectedAt,
            )
        val envelope =
            ClusterBroadcastEnvelope(
                envelopeId = "e1",
                sourceNodeId = nodeId,
                channel = "chat/general",
                event = "message",
                payload = """{"body":"hello"}""",
                createdAt = connectedAt,
            )

        assertEquals("u1", session.userId)
        assertEquals("chat/general", membership.channel)
        assertEquals("Mina", presence.metadata.getValue("displayName"))
        assertEquals("message", envelope.event)
    }
}
