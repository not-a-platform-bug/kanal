package io.github.kimseungjin.kanal.samples.chatpresence

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import io.github.kimseungjin.kanal.core.RealtimeApplication
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(
    classes = [ChatPresenceApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class ChatPresenceApplicationTest(
    @Autowired private val realtimeApplication: RealtimeApplication,
    @LocalServerPort private val port: Int,
) {
    @Test
    fun `registers chat and typing channels`() {
        assertNotNull(realtimeApplication)

        val descriptions = realtimeApplication.describe()

        assertEquals(
            listOf(
                "chat/{roomId} <ChatMessage> - Room chat messages with presence-aware join and leave hooks",
                "chat/{roomId}/typing <TypingSignal> - Ephemeral typing indicators for a chat room",
            ),
            descriptions,
        )
    }

    @Test
    fun `uses different backpressure policies for durable chat and ephemeral typing events`() {
        val channelsByPattern = realtimeApplication.channels.associateBy { it.pattern.value }

        assertEquals(BackpressurePolicy.DROP_OLDEST, channelsByPattern.getValue("chat/{roomId}").backpressurePolicy)
        assertEquals(BackpressurePolicy.DROP_LATEST, channelsByPattern.getValue("chat/{roomId}/typing").backpressurePolicy)
    }

    @Test
    fun `exposes kanal actuator diagnostics`() {
        val response =
            HttpClient
                .newHttpClient()
                .send(
                    HttpRequest
                        .newBuilder(URI.create("http://localhost:$port/actuator/kanal"))
                        .GET()
                        .build(),
                    HttpResponse.BodyHandlers.ofString(),
                )

        assertEquals(200, response.statusCode())
        val json = jacksonObjectMapper().readTree(response.body())
        assertNotNull(json["metrics"])
        assertNotNull(json["sessions"])
    }

    @Test
    fun `runs chat presence flow through the websocket endpoint`() {
        assertTimeoutPreemptively(Duration.ofSeconds(10)) {
            val listener = RecordingWebSocketListener()
            val socket =
                HttpClient
                    .newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:$port/realtime"), listener)
                    .join()

            socket.sendText(
                """
                {
                  "ref": "1",
                  "event": "join",
                  "channel": "chat/general",
                  "payload": {}
                }
                """.trimIndent(),
                true,
            ).join()

            socket.sendText(
                """
                {
                  "ref": "2",
                  "event": "message",
                  "channel": "chat/general",
                  "payload": {
                    "messageId": "m1",
                    "body": "hello"
                  }
                }
                """.trimIndent(),
                true,
            ).join()

            assertTrue(listener.awaitMessages())
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "test complete").join()

            val frames = listener.jsonFrames()
            assertTrue(frames.any { it["event"].asText() == "reply" && it["payload"]["event"].asText() == "join" })
            assertTrue(frames.any { it["event"].asText() == "message" && it["payload"]?.get("message")?.asText()?.startsWith("Welcome") == true })
            assertTrue(frames.any { it["event"].asText() == "message" && it["payload"]?.get("body")?.asText() == "hello" })
        }
    }

    private class RecordingWebSocketListener : WebSocket.Listener {
        private val mapper = jacksonObjectMapper()
        private val latch = CountDownLatch(4)
        private val messages = mutableListOf<String>()

        override fun onOpen(webSocket: WebSocket) {
            webSocket.request(1)
        }

        override fun onText(
            webSocket: WebSocket,
            data: CharSequence,
            last: Boolean,
        ): CompletionStage<*> {
            synchronized(messages) {
                messages += data.toString()
            }
            latch.countDown()
            webSocket.request(1)
            return CompletableFuture.completedFuture(null)
        }

        fun awaitMessages(): Boolean =
            latch.await(5, TimeUnit.SECONDS)

        fun jsonFrames(): List<JsonNode> =
            synchronized(messages) {
                messages.map { mapper.readTree(it) }
            }
    }
}
