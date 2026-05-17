package io.github.kimseungjin.kanal.runtime

import io.github.kimseungjin.kanal.core.dsl.channel
import io.github.kimseungjin.kanal.core.dsl.realtime
import kotlin.test.Test
import kotlin.test.assertEquals

class MeasuredChannelResolverTest {
    data class Message(val body: String)

    @Test
    fun `records channel resolution metrics`() {
        val app =
            realtime {
                channel<Message>("chat/{roomId}") {}
            }
        val metrics = RuntimeMetrics()
        val resolver = MeasuredChannelResolver(app, metrics)

        val resolution = resolver.resolve("chat/alpha")

        assertEquals(mapOf("roomId" to "alpha"), resolution?.address?.parameters)
        assertEquals(1, metrics.snapshot().channelResolutionCount)
    }
}
