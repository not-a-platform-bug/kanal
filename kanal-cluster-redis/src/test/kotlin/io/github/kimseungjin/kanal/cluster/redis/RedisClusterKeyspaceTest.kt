package io.github.kimseungjin.kanal.cluster.redis

import kotlin.test.Test
import kotlin.test.assertEquals

class RedisClusterKeyspaceTest {
    @Test
    fun `builds stable redis keys`() {
        val keyspace = RedisClusterKeyspace("kanal-test")

        assertEquals("kanal-test:nodes:node-a", keyspace.node(ClusterNodeId("node-a")))
        assertEquals("kanal-test:sessions:s1", keyspace.session("s1"))
        assertEquals("kanal-test:users:u1:sessions", keyspace.userSessions("u1"))
        assertEquals("kanal-test:channels:chat~general:members", keyspace.channelMembers("chat/general"))
        assertEquals("kanal-test:channels:chat~general:presence", keyspace.channelPresence("chat/general"))
        assertEquals("kanal-test:broadcasts", keyspace.broadcastStream())
    }
}
