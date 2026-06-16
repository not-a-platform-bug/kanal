package io.github.kimseungjin.kanal.cluster.redis

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultRedisClusterMetadataStoreTest {
    @Test
    fun `stores and reads cluster metadata through redis commands`() {
        val client = FakeRedisCommandClient()
        val store = DefaultRedisClusterMetadataStore(client)
        val nodeId = ClusterNodeId("node-a")

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
                payload = "{\"body\":\"hello\"}",
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
    fun `removes session from user index`() {
        val store = DefaultRedisClusterMetadataStore(FakeRedisCommandClient())
        val nodeId = ClusterNodeId("node-a")

        store.putSession(ClusterSessionDescriptor(sessionId = "s1", nodeId = nodeId, userId = "u1"))
        store.removeSession("s1")

        assertNull(store.session("s1"))
        assertEquals(emptyList(), store.userSessions("u1"))
    }

    @Test
    fun `leaves channel and untracks presence by decoded metadata`() {
        val store = DefaultRedisClusterMetadataStore(FakeRedisCommandClient())
        val nodeId = ClusterNodeId("node-a")

        store.joinChannel(ClusterChannelMembership(channel = "chat/general", sessionId = "s1", nodeId = nodeId))
        store.trackPresence(ClusterPresenceEntry(channel = "chat/general", key = "u1", nodeId = nodeId, sessionId = "s1"))

        store.leaveChannel("chat/general", "s1")
        store.untrackPresence("chat/general", "u1")

        assertEquals(emptyList(), store.channelMembers("chat/general"))
        assertEquals(emptyList(), store.presence("chat/general"))
    }

    private class FakeRedisCommandClient : RedisCommandClient {
        private val values = mutableMapOf<String, String>()
        private val sets = mutableMapOf<String, MutableSet<String>>()
        private val streams = mutableMapOf<String, MutableList<String>>()

        override fun set(
            key: String,
            value: String,
            ttl: Duration,
        ) {
            values[key] = value
        }

        override fun get(key: String): String? = values[key]

        override fun delete(key: String) {
            values.remove(key)
            sets.remove(key)
            streams.remove(key)
        }

        override fun addToSet(
            key: String,
            member: String,
        ) {
            sets.getOrPut(key) { linkedSetOf() } += member
        }

        override fun removeFromSet(
            key: String,
            member: String,
        ) {
            sets[key]?.remove(member)
        }

        override fun setMembers(key: String): Set<String> = sets[key].orEmpty()

        override fun appendToStream(
            key: String,
            value: String,
            maxLength: Long,
        ) {
            val stream = streams.getOrPut(key) { mutableListOf() }
            stream += value
            while (stream.size > maxLength) {
                stream.removeFirst()
            }
        }

        override fun streamEntries(key: String): List<String> = streams[key].orEmpty()
    }
}
