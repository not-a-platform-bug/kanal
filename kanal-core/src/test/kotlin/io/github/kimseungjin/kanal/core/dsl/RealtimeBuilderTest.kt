package io.github.kimseungjin.kanal.core.dsl

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class RealtimeBuilderTest {
    data class ChatMessage(val body: String)

    @Test
    fun `registers typed channels through the DSL`() {
        val app =
            realtime {
                channel<ChatMessage>("chat/{roomId}") {
                    description("Room chat")
                    backpressure(BackpressurePolicy.DROP_OLDEST)
                }
            }

        val channel = app.channels.single()

        assertEquals("chat/{roomId}", channel.pattern.value)
        assertEquals(ChatMessage::class, channel.messageType)
        assertEquals("Room chat", channel.description)
        assertEquals(BackpressurePolicy.DROP_OLDEST, channel.backpressurePolicy)
    }
}
