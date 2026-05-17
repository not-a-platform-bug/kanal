package io.github.kimseungjin.kanal.core

class ChannelResolver(definitions: List<ChannelDefinition<*>>) {
    private val root = ResolverNode()

    init {
        definitions.forEach(::register)
    }

    fun resolve(path: String): ChannelResolution? {
        if (path.isBlank() || path.startsWith("/") || path.endsWith("/") || "//" in path) {
            return null
        }

        val parameters = mutableMapOf<String, String>()
        var node = root

        path.split("/").forEach { segment ->
            val staticNode = node.staticChildren[segment]
            if (staticNode != null) {
                node = staticNode
                return@forEach
            }

            val parameter = node.parameterChild ?: return null
            parameters[parameter.name] = segment
            node = parameter.node
        }

        val definition = node.definition ?: return null

        return ChannelResolution(
            definition = definition,
            address = ChannelAddress(
                pattern = definition.pattern,
                parameters = parameters.toMap(),
            ),
        )
    }

    private fun register(definition: ChannelDefinition<*>) {
        var node = root

        definition.pattern.segments.forEach { segment ->
            node =
                when (segment) {
                    is ChannelPatternSegment.Static ->
                        node.staticChildren.getOrPut(segment.value) { ResolverNode() }

                    is ChannelPatternSegment.Parameter ->
                        node.parameterChild(segment.name, definition.pattern.value)
                }
        }

        require(node.definition == null) {
            "Channel pattern '${definition.pattern.value}' duplicates '${node.definition?.pattern?.value}'"
        }

        node.definition = definition
    }

    private class ResolverNode {
        val staticChildren: MutableMap<String, ResolverNode> = linkedMapOf()
        var parameterChild: ParameterEdge? = null
        var definition: ChannelDefinition<*>? = null

        fun parameterChild(
            name: String,
            pattern: String,
        ): ResolverNode {
            val existing = parameterChild
            if (existing != null) {
                require(existing.name == name) {
                    "Channel pattern '$pattern' is ambiguous with another dynamic pattern at the same path segment"
                }

                return existing.node
            }

            val child = ResolverNode()
            parameterChild = ParameterEdge(name, child)
            return child
        }
    }

    private data class ParameterEdge(
        val name: String,
        val node: ResolverNode,
    )
}
