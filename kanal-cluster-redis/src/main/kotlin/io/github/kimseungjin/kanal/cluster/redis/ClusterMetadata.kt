package io.github.kimseungjin.kanal.cluster.redis

import java.time.Instant

@JvmInline
value class ClusterNodeId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Cluster node id must not be blank" }
    }
}

data class ClusterNodeDescriptor(
    val nodeId: ClusterNodeId,
    val advertisedHost: String,
    val startedAt: Instant = Instant.now(),
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(advertisedHost.isNotBlank()) { "Advertised host must not be blank" }
    }
}

data class ClusterSessionDescriptor(
    val sessionId: String,
    val nodeId: ClusterNodeId,
    val userId: String? = null,
    val connectedAt: Instant = Instant.now(),
    val attributes: Map<String, String> = emptyMap(),
) {
    init {
        require(sessionId.isNotBlank()) { "Session id must not be blank" }
    }
}

data class ClusterChannelMembership(
    val channel: String,
    val sessionId: String,
    val nodeId: ClusterNodeId,
    val joinedAt: Instant = Instant.now(),
) {
    init {
        require(channel.isNotBlank()) { "Channel must not be blank" }
        require(sessionId.isNotBlank()) { "Session id must not be blank" }
    }
}

data class ClusterPresenceEntry(
    val channel: String,
    val key: String,
    val nodeId: ClusterNodeId,
    val sessionId: String,
    val metadata: Map<String, String> = emptyMap(),
    val joinedAt: Instant = Instant.now(),
) {
    init {
        require(channel.isNotBlank()) { "Channel must not be blank" }
        require(key.isNotBlank()) { "Presence key must not be blank" }
        require(sessionId.isNotBlank()) { "Session id must not be blank" }
    }
}

data class ClusterBroadcastEnvelope(
    val envelopeId: String,
    val sourceNodeId: ClusterNodeId,
    val channel: String,
    val event: String,
    val payload: String,
    val createdAt: Instant = Instant.now(),
) {
    init {
        require(envelopeId.isNotBlank()) { "Envelope id must not be blank" }
        require(channel.isNotBlank()) { "Channel must not be blank" }
        require(event.isNotBlank()) { "Event must not be blank" }
    }
}
