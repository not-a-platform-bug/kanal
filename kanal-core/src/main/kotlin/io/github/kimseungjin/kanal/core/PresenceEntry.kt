package io.github.kimseungjin.kanal.core

import java.time.Instant

data class PresenceEntry(
    val key: String,
    val metadata: Map<String, String> = emptyMap(),
    val joinedAt: Instant = Instant.now(),
)
