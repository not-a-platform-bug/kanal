package io.github.kimseungjin.kanal.core

data class ChannelAddress(
    val pattern: ChannelPattern,
    val parameters: Map<String, String> = emptyMap(),
)
