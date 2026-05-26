package io.github.kimseungjin.kanal.runtime

object RuntimeDisconnectReasons {
    const val CLOSED = "closed"
    const val RUNTIME_CLOSED = "runtime closed"
    const val HEARTBEAT_TIMEOUT = "heartbeat timeout"
    const val BACKPRESSURE = "backpressure"

    val standard: List<String> =
        listOf(
            CLOSED,
            RUNTIME_CLOSED,
            HEARTBEAT_TIMEOUT,
            BACKPRESSURE,
        )
}
