package io.github.kimseungjin.kanal.core

data class SessionDescriptor(
    val id: String,
    val userId: String? = null,
    val attributes: Map<String, String> = emptyMap(),
)
