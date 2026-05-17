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
)
