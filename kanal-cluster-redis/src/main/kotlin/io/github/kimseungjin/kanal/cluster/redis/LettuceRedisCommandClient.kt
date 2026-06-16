package io.github.kimseungjin.kanal.cluster.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.Range
import io.lettuce.core.SetArgs
import io.lettuce.core.XAddArgs
import io.lettuce.core.api.StatefulRedisConnection
import java.time.Duration

class LettuceRedisCommandClient(
    private val connection: StatefulRedisConnection<String, String>,
    private val ownedClient: RedisClient? = null,
) : RedisCommandClient,
    AutoCloseable {
    private val commands = connection.sync()

    constructor(redisUri: String) : this(RedisClient.create(redisUri))

    private constructor(client: RedisClient) : this(client.connect(), client)

    override fun set(
        key: String,
        value: String,
        ttl: Duration,
    ) {
        commands.set(key, value, SetArgs.Builder.px(ttl.toMillis()))
    }

    override fun get(key: String): String? = commands.get(key)

    override fun delete(key: String) {
        commands.del(key)
    }

    override fun addToSet(
        key: String,
        member: String,
    ) {
        commands.sadd(key, member)
    }

    override fun removeFromSet(
        key: String,
        member: String,
    ) {
        commands.srem(key, member)
    }

    override fun setMembers(key: String): Set<String> = commands.smembers(key)

    override fun appendToStream(
        key: String,
        value: String,
        maxLength: Long,
    ) {
        commands.xadd(key, XAddArgs.Builder.maxlen(maxLength).approximateTrimming(), mapOf("payload" to value))
    }

    override fun streamEntries(key: String): List<String> =
        commands.xrange(key, Range.unbounded<String>()).mapNotNull { message -> message.body["payload"] }

    override fun close() {
        connection.close()
        ownedClient?.shutdown()
    }
}
