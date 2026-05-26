package io.github.kimseungjin.kanal.core

import kotlin.reflect.KClass

class ChannelDefinition<T : Any>(
    val pattern: ChannelPattern,
    val messageType: KClass<T>,
    val description: String?,
    val backpressurePolicy: BackpressurePolicy,
    internal val joinHandler: ChannelContext.() -> Unit,
    internal val leaveHandler: ChannelContext.() -> Unit,
    internal val messageHandler: ChannelContext.(T) -> Unit,
) {
    fun invokeJoin(context: ChannelContext) {
        context.joinHandler()
    }

    fun invokeLeave(context: ChannelContext) {
        context.leaveHandler()
    }

    fun invokeMessage(
        context: ChannelContext,
        message: Any,
    ) {
        require(messageType.isInstance(message)) {
            "Message for '${pattern.value}' must be ${messageType.qualifiedName}, but was ${message::class.qualifiedName}"
        }

        @Suppress("UNCHECKED_CAST")
        context.messageHandler(message as T)
    }
}
