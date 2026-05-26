package io.github.kimseungjin.kanal.spring

import io.github.kimseungjin.kanal.runtime.RuntimePayloadCodec
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

class JacksonRuntimePayloadCodec(
    private val objectMapper: ObjectMapper,
) : RuntimePayloadCodec {
    override fun <T : Any> decode(
        payload: Any?,
        type: KClass<T>,
    ): T {
        require(payload != null) { "Payload must not be null for ${type.qualifiedName}" }

        return when {
            type.isInstance(payload) -> {
                @Suppress("UNCHECKED_CAST")
                payload as T
            }

            payload is JsonNode -> objectMapper.treeToValue(payload, type.java)
            else -> objectMapper.convertValue(payload, type.java)
        }
    }

    override fun encode(message: Any): Any = objectMapper.valueToTree<JsonNode>(message)

    companion object {
        fun default(): JacksonRuntimePayloadCodec = JacksonRuntimePayloadCodec(jacksonObjectMapper())
    }
}
