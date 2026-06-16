package io.github.kimseungjin.kanal.cluster.redis

class DefaultRedisClusterMetadataStore(
    private val client: RedisCommandClient,
    private val options: RedisClusterOptions = RedisClusterOptions(),
) : RedisClusterMetadataStore {
    private val json: RedisClusterMetadataJson = DelimitedRedisClusterMetadataJson
    private val keyspace = options.keyspace()

    override fun registerNode(node: ClusterNodeDescriptor) {
        client.set(keyspace.node(node.nodeId), json.encodeNode(node), options.nodeTtl)
    }

    override fun unregisterNode(nodeId: ClusterNodeId) {
        client.delete(keyspace.node(nodeId))
    }

    override fun putSession(session: ClusterSessionDescriptor) {
        client.set(keyspace.session(session.sessionId), json.encodeSession(session), options.sessionTtl)
        session.userId?.let { client.addToSet(keyspace.userSessions(it), session.sessionId) }
    }

    override fun removeSession(sessionId: String) {
        val session = session(sessionId)
        client.delete(keyspace.session(sessionId))
        session?.userId?.let { client.removeFromSet(keyspace.userSessions(it), sessionId) }
    }

    override fun joinChannel(membership: ClusterChannelMembership) {
        client.addToSet(keyspace.channelMembers(membership.channel), json.encodeMembership(membership))
    }

    override fun leaveChannel(
        channel: String,
        sessionId: String,
    ) {
        channelMembers(channel)
            .filter { it.sessionId == sessionId }
            .forEach { client.removeFromSet(keyspace.channelMembers(channel), json.encodeMembership(it)) }
    }

    override fun trackPresence(entry: ClusterPresenceEntry) {
        client.addToSet(keyspace.channelPresence(entry.channel), json.encodePresence(entry))
    }

    override fun untrackPresence(
        channel: String,
        key: String,
    ) {
        presence(channel)
            .filter { it.key == key }
            .forEach { client.removeFromSet(keyspace.channelPresence(channel), json.encodePresence(it)) }
    }

    override fun publish(envelope: ClusterBroadcastEnvelope) {
        client.appendToStream(keyspace.broadcastStream(), json.encodeBroadcast(envelope), options.broadcastStreamMaxLength)
    }

    override fun node(nodeId: ClusterNodeId): ClusterNodeDescriptor? =
        client.get(keyspace.node(nodeId))?.let(json::decodeNode)

    override fun session(sessionId: String): ClusterSessionDescriptor? =
        client.get(keyspace.session(sessionId))?.let(json::decodeSession)

    override fun channelMembers(channel: String): List<ClusterChannelMembership> =
        client.setMembers(keyspace.channelMembers(channel)).map(json::decodeMembership).sortedBy { it.sessionId }

    override fun userSessions(userId: String): List<ClusterSessionDescriptor> =
        client.setMembers(keyspace.userSessions(userId)).mapNotNull(::session).sortedBy { it.sessionId }

    override fun presence(channel: String): List<ClusterPresenceEntry> =
        client.setMembers(keyspace.channelPresence(channel)).map(json::decodePresence).sortedWith(compareBy({ it.key }, { it.sessionId }))

    override fun broadcasts(): List<ClusterBroadcastEnvelope> =
        client.streamEntries(keyspace.broadcastStream()).map(json::decodeBroadcast)
}
