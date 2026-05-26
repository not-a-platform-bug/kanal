package io.github.kimseungjin.kanal.cluster.redis

interface RedisClusterMetadataStore {
    fun registerNode(node: ClusterNodeDescriptor)

    fun unregisterNode(nodeId: ClusterNodeId)

    fun putSession(session: ClusterSessionDescriptor)

    fun removeSession(sessionId: String)

    fun joinChannel(membership: ClusterChannelMembership)

    fun leaveChannel(
        channel: String,
        sessionId: String,
    )

    fun trackPresence(entry: ClusterPresenceEntry)

    fun untrackPresence(
        channel: String,
        key: String,
    )

    fun publish(envelope: ClusterBroadcastEnvelope)
}
