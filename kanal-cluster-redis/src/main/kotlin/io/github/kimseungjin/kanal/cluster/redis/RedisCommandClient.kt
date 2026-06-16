package io.github.kimseungjin.kanal.cluster.redis

import java.time.Duration

interface RedisCommandClient {
    fun set(
        key: String,
        value: String,
        ttl: Duration,
    )

    fun get(key: String): String?

    fun delete(key: String)

    fun addToSet(
        key: String,
        member: String,
    )

    fun removeFromSet(
        key: String,
        member: String,
    )

    fun setMembers(key: String): Set<String>

    fun appendToStream(
        key: String,
        value: String,
        maxLength: Long,
    )

    fun streamEntries(key: String): List<String>
}
