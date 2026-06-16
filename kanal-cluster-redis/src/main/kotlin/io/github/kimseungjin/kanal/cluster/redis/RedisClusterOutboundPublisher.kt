package io.github.kimseungjin.kanal.cluster.redis

import io.github.kimseungjin.kanal.core.ChannelAddress
import io.github.kimseungjin.kanal.runtime.ClusterOutboundPublisher
import java.util.UUID

class RedisClusterOutboundPublisher(
    private val nodeId: ClusterNodeId,
    private val store: RedisClusterMetadataStore,
    private val payloadEncoder: (Any) -> String,
    private val envelopeId: () -> String = { UUID.randomUUID().toString() },
) : ClusterOutboundPublisher {
    override fun broadcast(
        address: ChannelAddress,
        channel: String,
        payload: Any,
    ) {
        store.publish(
            ClusterBroadcastEnvelope(
                envelopeId = envelopeId(),
                sourceNodeId = nodeId,
                channel = channel,
                event = "message",
                payload = payloadEncoder(payload),
            ),
        )
    }
}
