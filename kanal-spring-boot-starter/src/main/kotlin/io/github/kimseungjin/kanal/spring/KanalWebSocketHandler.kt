package io.github.kimseungjin.kanal.spring

import io.github.kimseungjin.kanal.core.SessionDescriptor
import io.github.kimseungjin.kanal.runtime.LocalRealtimeRuntime
import io.github.kimseungjin.kanal.runtime.RealtimeErrorPayload
import io.github.kimseungjin.kanal.runtime.RealtimeFrame
import io.github.kimseungjin.kanal.runtime.RealtimeErrorCodes
import io.github.kimseungjin.kanal.runtime.RealtimeFrameEvents
import io.github.kimseungjin.kanal.runtime.RuntimeTransportSession
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper

class KanalWebSocketHandler(
    private val runtime: LocalRealtimeRuntime,
) : TextWebSocketHandler() {
    private val objectMapper = jacksonObjectMapper()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        runtime.connect(
            session =
                SessionDescriptor(
                    id = session.id,
                    userId = session.principal?.name,
                    attributes = session.attributes.toStringMap(),
                ),
            transport = SpringWebSocketTransportSession(session, objectMapper),
        )
    }

    override fun handleTextMessage(
        session: WebSocketSession,
        message: TextMessage,
    ) {
        val frame =
            try {
                objectMapper.readValue(message.payload, IncomingRealtimeFrame::class.java)
            } catch (_: Throwable) {
                sendMalformedFrameError(session)
                return
            }

        runtime.receive(
            sessionId = session.id,
            frame =
                RealtimeFrame(
                    ref = frame.ref,
                    event = frame.event,
                    channel = frame.channel,
                    payload = frame.payload,
                ),
        )
    }

    override fun afterConnectionClosed(
        session: WebSocketSession,
        status: CloseStatus,
    ) {
        runtime.disconnect(session.id, status.reason ?: status.code.toString())
    }

    private data class IncomingRealtimeFrame(
        val ref: String? = null,
        val event: String,
        val channel: String? = null,
        val payload: JsonNode? = null,
    )

    private fun sendMalformedFrameError(session: WebSocketSession) {
        if (!session.isOpen) {
            return
        }

        val text =
            objectMapper.writeValueAsString(
                RealtimeFrame(
                    event = RealtimeFrameEvents.ERROR,
                    payload =
                        RealtimeErrorPayload(
                            code = RealtimeErrorCodes.MALFORMED_FRAME,
                            message = "Malformed frame",
                        ),
                ),
            )

        synchronized(session) {
            if (session.isOpen) {
                session.sendMessage(TextMessage(text))
            }
        }
    }

    private class SpringWebSocketTransportSession(
        private val session: WebSocketSession,
        private val objectMapper: ObjectMapper,
    ) : RuntimeTransportSession {
        override fun send(frame: RealtimeFrame) {
            if (!session.isOpen) {
                return
            }

            val text = objectMapper.writeValueAsString(frame)
            synchronized(session) {
                if (session.isOpen) {
                    session.sendMessage(TextMessage(text))
                }
            }
        }

        override fun close(reason: String) {
            if (session.isOpen) {
                session.close(CloseStatus(CloseStatus.POLICY_VIOLATION.code, reason))
            }
        }
    }
}

private fun Map<String, Any>.toStringMap(): Map<String, String> =
    mapValues { (_, value) -> value.toString() }
