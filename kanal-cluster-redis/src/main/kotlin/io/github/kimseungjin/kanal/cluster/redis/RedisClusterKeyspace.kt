package io.github.kimseungjin.kanal.cluster.redis

class RedisClusterKeyspace(
    private val prefix: String = "kanal",
) {
    init {
        require(prefix.isNotBlank()) { "Redis key prefix must not be blank" }
    }

    fun node(nodeId: ClusterNodeId): String =
        "$prefix:nodes:${nodeId.value}"

    fun session(sessionId: String): String {
        require(sessionId.isNotBlank()) { "Session id must not be blank" }
        return "$prefix:sessions:$sessionId"
    }

    fun userSessions(userId: String): String {
        require(userId.isNotBlank()) { "User id must not be blank" }
        return "$prefix:users:$userId:sessions"
    }

    fun channelMembers(channel: String): String {
        require(channel.isNotBlank()) { "Channel must not be blank" }
        return "$prefix:channels:${channel.encodeKeyPart()}:members"
    }

    fun channelPresence(channel: String): String {
        require(channel.isNotBlank()) { "Channel must not be blank" }
        return "$prefix:channels:${channel.encodeKeyPart()}:presence"
    }

    fun broadcastStream(): String =
        "$prefix:broadcasts"

    private fun String.encodeKeyPart(): String =
        replace("/", "~")
}
