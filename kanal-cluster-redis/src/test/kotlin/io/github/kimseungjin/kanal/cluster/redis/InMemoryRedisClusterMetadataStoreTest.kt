package io.github.kimseungjin.kanal.cluster.redis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryRedisClusterMetadataStoreTest {
    @Test
    fun `stores node session membership presence and broadcast metadata`() {
        val nodeId = ClusterNodeId("node-a")
        val store = InMemoryRedisClusterMetadataStore()

        store.registerNode(ClusterNodeDescriptor(nodeId = nodeId, advertisedHost = "127.0.0.1:8080"))
        store.putSession(ClusterSessionDescriptor(sessionId = "s1", nodeId = nodeId, userId = "u1"))
        store.joinChannel(ClusterChannelMembership(channel = "chat/general", sessionId = "s1", nodeId = nodeId))
        store.trackPresence(ClusterPresenceEntry(channel = "chat/general", key = "u1", nodeId = nodeId, sessionId = "s1"))
        store.publish(
            ClusterBroadcastEnvelope(
                envelopeId = "e1",
                sourceNodeId = nodeId,
                channel = "chat/general",
                event = "message",
                payload = "{}",
            ),
        )

        assertEquals("127.0.0.1:8080", store.node(nodeId)?.advertisedHost)
        assertEquals("u1", store.session("s1")?.userId)
        assertEquals(listOf("s1"), store.channelMembers("chat/general").map { it.sessionId })
        assertEquals(listOf("s1"), store.userSessions("u1").map { it.sessionId })
        assertEquals(listOf("u1"), store.presence("chat/general").map { it.key })
        assertEquals(listOf("e1"), store.broadcasts().map { it.envelopeId })
    }

    @Test
    fun `removing a session clears routing and presence metadata`() {
        val nodeId = ClusterNodeId("node-a")
        val store = InMemoryRedisClusterMetadataStore()

        store.putSession(ClusterSessionDescriptor(sessionId = "s1", nodeId = nodeId, userId = "u1"))
        store.joinChannel(ClusterChannelMembership(channel = "chat/general", sessionId = "s1", nodeId = nodeId))
        store.trackPresence(ClusterPresenceEntry(channel = "chat/general", key = "u1", nodeId = nodeId, sessionId = "s1"))

        store.removeSession("s1")

        assertNull(store.session("s1"))
        assertEquals(emptyList(), store.channelMembers("chat/general"))
        assertEquals(emptyList(), store.presence("chat/general"))
    }

    @Test
    fun `unregistering a node removes owned sessions and metadata`() {
        val nodeId = ClusterNodeId("node-a")
        val store = InMemoryRedisClusterMetadataStore()

        store.registerNode(ClusterNodeDescriptor(nodeId = nodeId, advertisedHost = "127.0.0.1:8080"))
        store.putSession(ClusterSessionDescriptor(sessionId = "s1", nodeId = nodeId, userId = "u1"))
        store.joinChannel(ClusterChannelMembership(channel = "chat/general", sessionId = "s1", nodeId = nodeId))

        store.unregisterNode(nodeId)

        assertNull(store.node(nodeId))
        assertNull(store.session("s1"))
        assertEquals(emptyList(), store.channelMembers("chat/general"))
    }
}
