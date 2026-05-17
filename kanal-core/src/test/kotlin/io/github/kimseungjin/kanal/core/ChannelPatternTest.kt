package io.github.kimseungjin.kanal.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class ChannelPatternTest {
    @Test
    fun `compiles static and parameter segments`() {
        val pattern = ChannelPattern("chat/{roomId}/messages")

        assertIs<ChannelPatternSegment.Static>(pattern.segments[0])
        assertIs<ChannelPatternSegment.Parameter>(pattern.segments[1])
        assertIs<ChannelPatternSegment.Static>(pattern.segments[2])
        assertEquals("roomId", (pattern.segments[1] as ChannelPatternSegment.Parameter).name)
    }

    @Test
    fun `matches concrete paths and extracts parameters`() {
        val address = ChannelPattern("chat/{roomId}").match("chat/alpha")

        assertEquals(mapOf("roomId" to "alpha"), address?.parameters)
    }

    @Test
    fun `returns null for non matching paths`() {
        val pattern = ChannelPattern("chat/{roomId}")

        assertNull(pattern.match("notifications/alpha"))
        assertNull(pattern.match("chat/alpha/messages"))
        assertNull(pattern.match("/chat/alpha"))
    }

    @Test
    fun `rejects invalid patterns during construction`() {
        assertFailsWith<IllegalArgumentException> { ChannelPattern("") }
        assertFailsWith<IllegalArgumentException> { ChannelPattern("/chat/{roomId}") }
        assertFailsWith<IllegalArgumentException> { ChannelPattern("chat/{roomId}/") }
        assertFailsWith<IllegalArgumentException> { ChannelPattern("chat//{roomId}") }
        assertFailsWith<IllegalArgumentException> { ChannelPattern("chat/{room-id}") }
        assertFailsWith<IllegalArgumentException> { ChannelPattern("chat/{roomId}/{roomId}") }
    }
}
