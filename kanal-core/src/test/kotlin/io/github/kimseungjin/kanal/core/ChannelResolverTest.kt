package io.github.kimseungjin.kanal.core

import io.github.kimseungjin.kanal.core.dsl.channel
import io.github.kimseungjin.kanal.core.dsl.realtime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ChannelResolverTest {
    data class Message(val body: String)

    @Test
    fun `resolves dynamic channel patterns`() {
        val app =
            realtime {
                channel<Message>("chat/{roomId}") {}
            }

        val resolution = app.resolve("chat/alpha")

        assertEquals("chat/{roomId}", resolution?.definition?.pattern?.value)
        assertEquals(mapOf("roomId" to "alpha"), resolution?.address?.parameters)
    }

    @Test
    fun `prefers static path segments over dynamic parameters`() {
        val app =
            realtime {
                channel<Message>("chat/{roomId}") {}
                channel<Message>("chat/system") {}
            }

        assertEquals("chat/system", app.resolve("chat/system")?.definition?.pattern?.value)
        assertEquals("chat/{roomId}", app.resolve("chat/general")?.definition?.pattern?.value)
    }

    @Test
    fun `returns null when no channel matches`() {
        val app =
            realtime {
                channel<Message>("chat/{roomId}") {}
            }

        assertNull(app.resolve("presence/general"))
        assertNull(app.resolve("chat/general/extra"))
    }

    @Test
    fun `rejects duplicate channel registrations`() {
        assertFailsWith<IllegalArgumentException> {
            realtime {
                channel<Message>("chat/{roomId}") {}
                channel<Message>("chat/{roomId}") {}
            }
        }
    }

    @Test
    fun `rejects ambiguous dynamic channel registrations`() {
        assertFailsWith<IllegalArgumentException> {
            realtime {
                channel<Message>("chat/{roomId}") {}
                channel<Message>("chat/{id}") {}
            }
        }
    }
}
