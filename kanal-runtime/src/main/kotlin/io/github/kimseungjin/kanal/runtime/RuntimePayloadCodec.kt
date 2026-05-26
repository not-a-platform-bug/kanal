package io.github.kimseungjin.kanal.runtime

import kotlin.reflect.KClass

interface RuntimePayloadCodec {
    fun <T : Any> decode(
        payload: Any?,
        type: KClass<T>,
    ): T

    fun encode(message: Any): Any
}

object IdentityRuntimePayloadCodec : RuntimePayloadCodec {
    override fun <T : Any> decode(
        payload: Any?,
        type: KClass<T>,
    ): T {
        require(payload != null) { "Payload must not be null for ${type.qualifiedName}" }
        require(type.isInstance(payload)) {
            "Payload must be ${type.qualifiedName}, but was ${payload::class.qualifiedName}"
        }

        @Suppress("UNCHECKED_CAST")
        return payload as T
    }

    override fun encode(message: Any): Any = message
}
