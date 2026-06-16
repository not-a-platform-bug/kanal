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

    fun node(nodeId: ClusterNodeId): ClusterNodeDescriptor?

    fun session(sessionId: String): ClusterSessionDescriptor?

    fun channelMembers(channel: String): List<ClusterChannelMembership>

    fun userSessions(userId: String): List<ClusterSessionDescriptor>

    fun presence(channel: String): List<ClusterPresenceEntry>

    fun broadcasts(): List<ClusterBroadcastEnvelope>
}
