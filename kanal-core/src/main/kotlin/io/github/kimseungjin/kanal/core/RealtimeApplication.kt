package io.github.kimseungjin.kanal.core

class RealtimeApplication internal constructor(
    val channels: List<ChannelDefinition<*>>,
) {
    private val resolver = ChannelResolver(channels)

    fun describe(): List<String> =
        channels.map { definition ->
            val description = definition.description?.let { " - $it" }.orEmpty()
            "${definition.pattern.value} <${definition.messageType.simpleName}>$description"
        }

    fun resolve(path: String): ChannelResolution? = resolver.resolve(path)
}
