package io.github.kimseungjin.kanal.cluster.redis

import java.util.concurrent.ConcurrentHashMap

class InMemoryRedisClusterMetadataStore : RedisClusterMetadataStore {
    private val nodes = ConcurrentHashMap<ClusterNodeId, ClusterNodeDescriptor>()
    private val sessions = ConcurrentHashMap<String, ClusterSessionDescriptor>()
    private val memberships = ConcurrentHashMap<ChannelSessionKey, ClusterChannelMembership>()
    private val presenceEntries = ConcurrentHashMap<ChannelPresenceKey, ClusterPresenceEntry>()
    private val broadcastEnvelopes = mutableListOf<ClusterBroadcastEnvelope>()
    private val broadcastLock = Any()

    override fun registerNode(node: ClusterNodeDescriptor) {
        nodes[node.nodeId] = node
    }

    override fun unregisterNode(nodeId: ClusterNodeId) {
        nodes.remove(nodeId)
        sessions.values.filter { it.nodeId == nodeId }.forEach { removeSession(it.sessionId) }
    }

    override fun putSession(session: ClusterSessionDescriptor) {
        sessions[session.sessionId] = session
    }

    override fun removeSession(sessionId: String) {
        sessions.remove(sessionId)
        memberships.keys.filter { it.sessionId == sessionId }.forEach(memberships::remove)
        presenceEntries.keys.filter { it.sessionId == sessionId }.forEach(presenceEntries::remove)
    }

    override fun joinChannel(membership: ClusterChannelMembership) {
        memberships[ChannelSessionKey(membership.channel, membership.sessionId)] = membership
    }

    override fun leaveChannel(
        channel: String,
        sessionId: String,
    ) {
        memberships.remove(ChannelSessionKey(channel, sessionId))
    }

    override fun trackPresence(entry: ClusterPresenceEntry) {
        presenceEntries[ChannelPresenceKey(entry.channel, entry.key, entry.sessionId)] = entry
    }

    override fun untrackPresence(
        channel: String,
        key: String,
    ) {
        presenceEntries.keys.filter { it.channel == channel && it.key == key }.forEach(presenceEntries::remove)
    }

    override fun publish(envelope: ClusterBroadcastEnvelope) {
        synchronized(broadcastLock) {
            broadcastEnvelopes += envelope
        }
    }

    override fun node(nodeId: ClusterNodeId): ClusterNodeDescriptor? = nodes[nodeId]

    override fun session(sessionId: String): ClusterSessionDescriptor? = sessions[sessionId]

    override fun channelMembers(channel: String): List<ClusterChannelMembership> =
        memberships.values.filter { it.channel == channel }.sortedBy { it.sessionId }

    override fun userSessions(userId: String): List<ClusterSessionDescriptor> =
        sessions.values.filter { it.userId == userId }.sortedBy { it.sessionId }

    override fun presence(channel: String): List<ClusterPresenceEntry> =
        presenceEntries.values.filter { it.channel == channel }.sortedWith(compareBy({ it.key }, { it.sessionId }))

    override fun broadcasts(): List<ClusterBroadcastEnvelope> = synchronized(broadcastLock) { broadcastEnvelopes.toList() }

    private data class ChannelSessionKey(
        val channel: String,
        val sessionId: String,
    )

    private data class ChannelPresenceKey(
        val channel: String,
        val key: String,
        val sessionId: String,
    )
}
