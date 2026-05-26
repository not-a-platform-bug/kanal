package io.github.kimseungjin.kanal.cluster.redis

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RedisClusterOptionsTest {
    @Test
    fun `creates keyspace from options`() {
        val options = RedisClusterOptions(keyPrefix = "kanal-prod")

        assertEquals("kanal-prod:broadcasts", options.keyspace().broadcastStream())
    }

    @Test
    fun `rejects invalid ttl and stream settings`() {
        assertFailsWith<IllegalArgumentException> {
            RedisClusterOptions(nodeTtl = Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            RedisClusterOptions(broadcastStreamMaxLength = 0)
        }
    }
}
