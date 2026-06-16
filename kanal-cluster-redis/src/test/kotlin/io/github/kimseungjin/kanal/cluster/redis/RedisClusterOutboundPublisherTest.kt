package io.github.kimseungjin.kanal.cluster.redis

import io.github.kimseungjin.kanal.core.ChannelAddress
import io.github.kimseungjin.kanal.core.ChannelPattern
import kotlin.test.Test
import kotlin.test.assertEquals

class RedisClusterOutboundPublisherTest {
    @Test
    fun `publishes broadcast envelopes to metadata store`() {
        val store = InMemoryRedisClusterMetadataStore()
        val publisher =
            RedisClusterOutboundPublisher(
                nodeId = ClusterNodeId("node-a"),
                store = store,
                payloadEncoder = { payload -> payload.toString() },
                envelopeId = { "e1" },
            )

        publisher.broadcast(
            address = ChannelAddress(ChannelPattern("chat/{roomId}"), mapOf("roomId" to "general")),
            channel = "chat/general",
            payload = mapOf("body" to "hello"),
        )

        val envelope = store.broadcasts().single()
        assertEquals("e1", envelope.envelopeId)
        assertEquals(ClusterNodeId("node-a"), envelope.sourceNodeId)
        assertEquals("chat/general", envelope.channel)
        assertEquals("message", envelope.event)
        assertEquals("{body=hello}", envelope.payload)
    }
}
