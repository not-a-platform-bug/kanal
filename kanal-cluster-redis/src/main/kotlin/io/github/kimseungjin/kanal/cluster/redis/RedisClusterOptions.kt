package io.github.kimseungjin.kanal.cluster.redis

import java.time.Duration

data class RedisClusterOptions(
    val keyPrefix: String = "kanal",
    val nodeTtl: Duration = Duration.ofSeconds(30),
    val sessionTtl: Duration = Duration.ofMinutes(2),
    val presenceTtl: Duration = Duration.ofMinutes(2),
    val broadcastStreamMaxLength: Long = 100_000,
) {
    init {
        require(keyPrefix.isNotBlank()) { "Redis key prefix must not be blank" }
        require(!nodeTtl.isNegative && !nodeTtl.isZero) { "Node TTL must be greater than zero" }
        require(!sessionTtl.isNegative && !sessionTtl.isZero) { "Session TTL must be greater than zero" }
        require(!presenceTtl.isNegative && !presenceTtl.isZero) { "Presence TTL must be greater than zero" }
        require(broadcastStreamMaxLength > 0) { "Broadcast stream max length must be greater than zero" }
    }

    fun keyspace(): RedisClusterKeyspace =
        RedisClusterKeyspace(keyPrefix)
}
