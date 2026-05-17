package io.github.kimseungjin.kanal.samples.chatpresence

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import io.github.kimseungjin.kanal.core.RealtimeApplication
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest(classes = [ChatPresenceApplication::class])
class ChatPresenceApplicationTest(
    @Autowired private val realtimeApplication: RealtimeApplication,
) {
    @Test
    fun `registers chat and typing channels`() {
        assertNotNull(realtimeApplication)

        val descriptions = realtimeApplication.describe()

        assertEquals(
            listOf(
                "chat/{roomId} <ChatMessage> - Room chat messages with presence-aware join and leave hooks",
                "chat/{roomId}/typing <TypingSignal> - Ephemeral typing indicators for a chat room",
            ),
            descriptions,
        )
    }

    @Test
    fun `uses different backpressure policies for durable chat and ephemeral typing events`() {
        val channelsByPattern = realtimeApplication.channels.associateBy { it.pattern.value }

        assertEquals(BackpressurePolicy.DROP_OLDEST, channelsByPattern.getValue("chat/{roomId}").backpressurePolicy)
        assertEquals(BackpressurePolicy.DROP_LATEST, channelsByPattern.getValue("chat/{roomId}/typing").backpressurePolicy)
    }
}
