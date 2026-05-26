package io.github.kimseungjin.kanal.spring

import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals

class JacksonRuntimePayloadCodecTest {
    data class Message(
        val body: String,
    )

    @Test
    fun `decodes json nodes into typed payloads`() {
        val mapper = jacksonObjectMapper()
        val codec = JacksonRuntimePayloadCodec(mapper)
        val payload = mapper.readTree("""{"body":"hello"}""")

        assertEquals(Message("hello"), codec.decode(payload, Message::class))
    }

    @Test
    fun `encodes typed payloads into json nodes`() {
        val mapper = jacksonObjectMapper()
        val codec = JacksonRuntimePayloadCodec(mapper)

        val payload = codec.encode(Message("hello"))

        assertEquals("""{"body":"hello"}""", mapper.writeValueAsString(payload))
    }
}
