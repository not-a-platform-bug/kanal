package io.github.kimseungjin.kanal.core

import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryPresenceStoreTest {
    @Test
    fun `tracks and removes presence entries per channel`() {
        val store = InMemoryPresenceStore()
        val channel = ChannelAddress(ChannelPattern("chat/{roomId}"), mapOf("roomId" to "alpha"))

        store.track(channel, PresenceEntry("u1", mapOf("device" to "ios")))
        store.track(channel, PresenceEntry("u2", mapOf("device" to "android")))

        assertEquals(2, store.list(channel).size)

        store.untrack(channel, "u1")

        assertEquals(listOf("u2"), store.list(channel).map { it.key })
    }
}
