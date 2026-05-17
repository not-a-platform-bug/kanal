package io.github.kimseungjin.kanal.core

data class ChannelPattern(val value: String) {
    init {
        require(value.isNotBlank()) { "Channel pattern must not be blank" }
        require(!value.startsWith("/")) { "Channel pattern must not start with '/'" }
        require(!value.endsWith("/")) { "Channel pattern must not end with '/'" }
        require("//" !in value) { "Channel pattern must not contain empty path segments" }
    }

    val segments: List<ChannelPatternSegment> = compile(value)

    fun match(path: String): ChannelAddress? {
        if (path.isBlank() || path.startsWith("/") || path.endsWith("/") || "//" in path) {
            return null
        }

        val pathSegments = path.split("/")
        if (pathSegments.size != segments.size) {
            return null
        }

        val parameters = mutableMapOf<String, String>()

        segments.zip(pathSegments).forEach { (patternSegment, pathSegment) ->
            when (patternSegment) {
                is ChannelPatternSegment.Static -> {
                    if (patternSegment.value != pathSegment) {
                        return null
                    }
                }

                is ChannelPatternSegment.Parameter -> {
                    parameters[patternSegment.name] = pathSegment
                }
            }
        }

        return ChannelAddress(pattern = this, parameters = parameters.toMap())
    }

    private companion object {
        private val parameterName = Regex("[A-Za-z][A-Za-z0-9_]*")

        private fun compile(value: String): List<ChannelPatternSegment> {
            val seenParameters = mutableSetOf<String>()

            return value.split("/").map { segment ->
                require(segment.isNotBlank()) { "Channel pattern must not contain empty path segments" }

                if (segment.startsWith("{") || segment.endsWith("}")) {
                    require(segment.startsWith("{") && segment.endsWith("}")) {
                        "Channel parameter segment must use the form '{name}'"
                    }

                    val name = segment.removePrefix("{").removeSuffix("}")
                    require(parameterName.matches(name)) {
                        "Channel parameter name must start with a letter and contain only letters, digits, or '_'"
                    }
                    require(seenParameters.add(name)) {
                        "Channel parameter '$name' must not be declared more than once"
                    }

                    ChannelPatternSegment.Parameter(name)
                } else {
                    ChannelPatternSegment.Static(segment)
                }
            }
        }
    }
}

sealed interface ChannelPatternSegment {
    data class Static(val value: String) : ChannelPatternSegment

    data class Parameter(val name: String) : ChannelPatternSegment
}
