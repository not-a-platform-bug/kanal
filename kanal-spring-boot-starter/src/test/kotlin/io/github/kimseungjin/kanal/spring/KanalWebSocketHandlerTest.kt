package io.github.kimseungjin.kanal.spring

import io.github.kimseungjin.kanal.core.InMemoryPresenceStore
import io.github.kimseungjin.kanal.core.dsl.realtime
import io.github.kimseungjin.kanal.runtime.LocalRealtimeRuntime
import org.springframework.http.HttpHeaders
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketExtension
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import java.net.InetSocketAddress
import java.net.URI
import java.security.Principal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KanalWebSocketHandlerTest {
    @Test
    fun `malformed json frames return an error frame`() {
        val runtime = LocalRealtimeRuntime(realtime {}, InMemoryPresenceStore())
        val handler = KanalWebSocketHandler(runtime)
        val session = RecordingWebSocketSession()

        handler.afterConnectionEstablished(session)
        handler.handleMessage(session, TextMessage("{"))

        assertEquals(1, session.sent.size)
        assertTrue(session.sent.single().contains("\"event\":\"error\""))
        assertTrue(session.sent.single().contains("Malformed frame"))

        runtime.close()
    }

    private class RecordingWebSocketSession : WebSocketSession {
        val sent = mutableListOf<String>()
        private var open = true

        override fun getId(): String = "ws-1"

        override fun getUri(): URI = URI.create("ws://localhost/realtime")

        override fun getHandshakeHeaders(): HttpHeaders = HttpHeaders()

        override fun getAttributes(): MutableMap<String, Any> = mutableMapOf()

        override fun getPrincipal(): Principal? = null

        override fun getLocalAddress(): InetSocketAddress? = null

        override fun getRemoteAddress(): InetSocketAddress? = null

        override fun getAcceptedProtocol(): String? = null

        override fun setTextMessageSizeLimit(messageSizeLimit: Int) {
        }

        override fun getTextMessageSizeLimit(): Int = 64 * 1024

        override fun setBinaryMessageSizeLimit(messageSizeLimit: Int) {
        }

        override fun getBinaryMessageSizeLimit(): Int = 64 * 1024

        override fun getExtensions(): List<WebSocketExtension> = emptyList()

        override fun sendMessage(message: WebSocketMessage<*>) {
            sent += message.payload.toString()
        }

        override fun isOpen(): Boolean = open

        override fun close() {
            open = false
        }

        override fun close(status: CloseStatus) {
            open = false
        }
    }
}
