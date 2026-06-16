package io.github.kimseungjin.kanal.cluster.redis

internal interface RedisClusterMetadataJson {
    fun encodeNode(node: ClusterNodeDescriptor): String
    fun decodeNode(value: String): ClusterNodeDescriptor
    fun encodeSession(session: ClusterSessionDescriptor): String
    fun decodeSession(value: String): ClusterSessionDescriptor
    fun encodeMembership(membership: ClusterChannelMembership): String
    fun decodeMembership(value: String): ClusterChannelMembership
    fun encodePresence(entry: ClusterPresenceEntry): String
    fun decodePresence(value: String): ClusterPresenceEntry
    fun encodeBroadcast(envelope: ClusterBroadcastEnvelope): String
    fun decodeBroadcast(value: String): ClusterBroadcastEnvelope
}

internal object DelimitedRedisClusterMetadataJson : RedisClusterMetadataJson {
    override fun encodeNode(node: ClusterNodeDescriptor): String =
        encode(
            node.nodeId.value,
            node.advertisedHost,
            node.startedAt.toString(),
            encodeMap(node.metadata),
        )

    override fun decodeNode(value: String): ClusterNodeDescriptor {
        val parts = decode(value)
        return ClusterNodeDescriptor(
            nodeId = ClusterNodeId(parts[0]),
            advertisedHost = parts[1],
            startedAt = java.time.Instant.parse(parts[2]),
            metadata = decodeMap(parts[3]),
        )
    }

    override fun encodeSession(session: ClusterSessionDescriptor): String =
        encode(
            session.sessionId,
            session.nodeId.value,
            session.userId.orEmpty(),
            session.connectedAt.toString(),
            encodeMap(session.attributes),
        )

    override fun decodeSession(value: String): ClusterSessionDescriptor {
        val parts = decode(value)
        return ClusterSessionDescriptor(
            sessionId = parts[0],
            nodeId = ClusterNodeId(parts[1]),
            userId = parts[2].ifBlank { null },
            connectedAt = java.time.Instant.parse(parts[3]),
            attributes = decodeMap(parts[4]),
        )
    }

    override fun encodeMembership(membership: ClusterChannelMembership): String =
        encode(
            membership.channel,
            membership.sessionId,
            membership.nodeId.value,
            membership.joinedAt.toString(),
        )

    override fun decodeMembership(value: String): ClusterChannelMembership {
        val parts = decode(value)
        return ClusterChannelMembership(
            channel = parts[0],
            sessionId = parts[1],
            nodeId = ClusterNodeId(parts[2]),
            joinedAt = java.time.Instant.parse(parts[3]),
        )
    }

    override fun encodePresence(entry: ClusterPresenceEntry): String =
        encode(
            entry.channel,
            entry.key,
            entry.nodeId.value,
            entry.sessionId,
            encodeMap(entry.metadata),
            entry.joinedAt.toString(),
        )

    override fun decodePresence(value: String): ClusterPresenceEntry {
        val parts = decode(value)
        return ClusterPresenceEntry(
            channel = parts[0],
            key = parts[1],
            nodeId = ClusterNodeId(parts[2]),
            sessionId = parts[3],
            metadata = decodeMap(parts[4]),
            joinedAt = java.time.Instant.parse(parts[5]),
        )
    }

    override fun encodeBroadcast(envelope: ClusterBroadcastEnvelope): String =
        encode(
            envelope.envelopeId,
            envelope.sourceNodeId.value,
            envelope.channel,
            envelope.event,
            envelope.payload,
            envelope.createdAt.toString(),
        )

    override fun decodeBroadcast(value: String): ClusterBroadcastEnvelope {
        val parts = decode(value)
        return ClusterBroadcastEnvelope(
            envelopeId = parts[0],
            sourceNodeId = ClusterNodeId(parts[1]),
            channel = parts[2],
            event = parts[3],
            payload = parts[4],
            createdAt = java.time.Instant.parse(parts[5]),
        )
    }

    private fun encode(vararg parts: String): String = parts.joinToString("|") { it.escape() }

    private fun decode(value: String): List<String> = value.split('|').map { it.unescape() }

    private fun encodeMap(values: Map<String, String>): String =
        values.toSortedMap().entries.joinToString("&") { (key, value) -> "${key.escape()}=${value.escape()}" }

    private fun decodeMap(value: String): Map<String, String> {
        if (value.isBlank()) {
            return emptyMap()
        }

        return value.split('&').associate { entry ->
            val index = entry.indexOf('=')
            require(index >= 0) { "Malformed map entry" }
            entry.substring(0, index).unescape() to entry.substring(index + 1).unescape()
        }
    }

    private fun String.escape(): String =
        replace("%", "%25")
            .replace("|", "%7C")
            .replace("&", "%26")
            .replace("=", "%3D")

    private fun String.unescape(): String =
        replace("%3D", "=")
            .replace("%26", "&")
            .replace("%7C", "|")
            .replace("%25", "%")
}
