package io.github.kimseungjin.kanal.core.dsl

import io.github.kimseungjin.kanal.core.ChannelDefinition
import io.github.kimseungjin.kanal.core.RealtimeApplication
import kotlin.reflect.KClass

@KanalDsl
class RealtimeBuilder {
    private val channels = mutableListOf<ChannelDefinition<*>>()

    fun <T : Any> channel(
        pattern: String,
        messageType: KClass<T>,
        block: ChannelBuilder<T>.() -> Unit,
    ) {
        val builder = ChannelBuilder(pattern, messageType)
        builder.block()
        channels += builder.build()
    }

    internal fun build(): RealtimeApplication = RealtimeApplication(channels.toList())
}

inline fun <reified T : Any> RealtimeBuilder.channel(
    pattern: String,
    noinline block: ChannelBuilder<T>.() -> Unit,
) {
    channel(pattern = pattern, messageType = T::class, block = block)
}

fun realtime(block: RealtimeBuilder.() -> Unit): RealtimeApplication =
    RealtimeBuilder().apply(block).build()
