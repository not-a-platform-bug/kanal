package io.github.kimseungjin.kanal.runtime

import java.util.ArrayDeque

class RuntimeEventLog(
    private val capacity: Int = 256,
) {
    private val lock = Any()
    private val events = ArrayDeque<RuntimeEvent>(capacity)

    init {
        require(capacity > 0) { "Runtime event log capacity must be greater than zero" }
    }

    fun record(
        type: String,
        sessionId: String? = null,
        channel: String? = null,
        detail: String? = null,
    ) {
        synchronized(lock) {
            if (events.size == capacity) {
                events.removeFirst()
            }
            events.addLast(
                RuntimeEvent(
                    sequence = nextSequence++,
                    type = type,
                    sessionId = sessionId,
                    channel = channel,
                    detail = detail,
                ),
            )
        }
    }

    fun snapshot(): List<RuntimeEvent> = synchronized(lock) { events.toList() }

    private var nextSequence = 1L
}

data class RuntimeEvent(
    val sequence: Long,
    val type: String,
    val sessionId: String?,
    val channel: String?,
    val detail: String?,
)

object RuntimeEventTypes {
    const val SESSION_CONNECTED = "session_connected"
    const val SESSION_DISCONNECTED = "session_disconnected"
    const val JOINED = "joined"
    const val LEFT = "left"
    const val HANDLER_FAILED = "handler_failed"
    const val PAYLOAD_DECODE_FAILED = "payload_decode_failed"
    const val HEARTBEAT_TIMEOUT = "heartbeat_timeout"
    const val BACKPRESSURE_DISCONNECT = "backpressure_disconnect"
}
