package io.github.kimseungjin.kanal.runtime

data class RealtimeFrame(
    val ref: String? = null,
    val event: String,
    val channel: String? = null,
    val payload: Any? = null,
)

object RealtimeFrameEvents {
    const val JOIN = "join"
    const val LEAVE = "leave"
    const val MESSAGE = "message"
    const val HEARTBEAT = "heartbeat"
    const val REPLY = "reply"
    const val ERROR = "error"
}

object RealtimeReplyStatus {
    const val OK = "ok"
}

data class RealtimeReplyPayload(
    val event: String,
    val status: String = RealtimeReplyStatus.OK,
    val response: Any? = null,
)

object RealtimeErrorCodes {
    const val MALFORMED_FRAME = "malformed_frame"
    const val UNKNOWN_EVENT = "unknown_event"
    const val MISSING_CHANNEL = "missing_channel"
    const val CHANNEL_NOT_FOUND = "channel_not_found"
    const val NOT_JOINED = "not_joined"
    const val PAYLOAD_DECODE_FAILED = "payload_decode_failed"
}

data class RealtimeErrorPayload(
    val code: String,
    val message: String,
)
