package io.github.kimseungjin.kanal.core.dsl

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import io.github.kimseungjin.kanal.core.ChannelContext
import io.github.kimseungjin.kanal.core.ChannelDefinition
import io.github.kimseungjin.kanal.core.ChannelPattern
import kotlin.reflect.KClass

@KanalDsl
class ChannelBuilder<T : Any>(
    private val pattern: String,
    private val messageType: KClass<T>,
) {
    private var description: String? = null
    private var backpressurePolicy: BackpressurePolicy = BackpressurePolicy.SUSPEND
    private var joinHandler: ChannelContext.() -> Unit = {}
    private var leaveHandler: ChannelContext.() -> Unit = {}
    private var messageHandler: ChannelContext.(T) -> Unit = {}

    fun description(value: String) {
        description = value
    }

    fun backpressure(policy: BackpressurePolicy) {
        backpressurePolicy = policy
    }

    fun onJoin(handler: ChannelContext.() -> Unit) {
        joinHandler = handler
    }

    fun onLeave(handler: ChannelContext.() -> Unit) {
        leaveHandler = handler
    }

    fun onMessage(handler: ChannelContext.(T) -> Unit) {
        messageHandler = handler
    }

    internal fun build(): ChannelDefinition<T> =
        ChannelDefinition(
            pattern = ChannelPattern(pattern),
            messageType = messageType,
            description = description,
            backpressurePolicy = backpressurePolicy,
            joinHandler = joinHandler,
            leaveHandler = leaveHandler,
            messageHandler = messageHandler,
        )
}
