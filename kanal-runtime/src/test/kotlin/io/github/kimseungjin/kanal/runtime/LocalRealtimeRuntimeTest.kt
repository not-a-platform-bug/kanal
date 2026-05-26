package io.github.kimseungjin.kanal.runtime

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import io.github.kimseungjin.kanal.core.InMemoryPresenceStore
import io.github.kimseungjin.kanal.core.SessionDescriptor
import io.github.kimseungjin.kanal.core.dsl.channel
import io.github.kimseungjin.kanal.core.dsl.realtime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalRealtimeRuntimeTest {
    data class ChatMessage(
        val body: String,
    )

    @Test
    fun `joins a channel and dispatches typed messages`() {
        val app =
            realtime {
                channel<ChatMessage>("chat/{roomId}") {
                    onJoin {
                        presence.track(session.id)
                    }

                    onMessage { message ->
                        broadcast(message.copy(body = "${address.parameters.getValue("roomId")}:${message.body}"))
                    }
                }
            }
        val runtime = LocalRealtimeRuntime(app, InMemoryPresenceStore())
        val transport = RecordingTransportSession()

        runtime.connect(SessionDescriptor(id = "s1"), transport)
        runtime.receive("s1", RealtimeFrame(ref = "1", event = RealtimeFrameEvents.JOIN, channel = "chat/general"))
        runtime.receive(
            "s1",
            RealtimeFrame(
                ref = "2",
                event = RealtimeFrameEvents.MESSAGE,
                channel = "chat/general",
                payload = ChatMessage("hello"),
            ),
        )

        assertTrue(
            transport.sent.any {
                it.event == RealtimeFrameEvents.REPLY &&
                    it.payload == RealtimeReplyPayload(event = RealtimeFrameEvents.JOIN, response = emptyMap<String, String>())
            },
        )
        assertTrue(
            transport.sent.any {
                it.event == RealtimeFrameEvents.MESSAGE && it.payload == ChatMessage("general:hello")
            },
        )
        assertEquals(1, runtime.snapshot().activeSessions)
        assertEquals(1, runtime.snapshot().activeMemberships)
    }

    @Test
    fun `broadcasts to all joined sessions with one resolved concrete channel`() {
        val app =
            realtime {
                channel<ChatMessage>("chat/{roomId}") {
                    onMessage { message ->
                        broadcast(message.copy(body = address.parameters.getValue("roomId") + ":" + message.body))
                    }
                }
            }
        val runtime = LocalRealtimeRuntime(app, InMemoryPresenceStore())
        val first = RecordingTransportSession()
        val second = RecordingTransportSession()

        runtime.connect(SessionDescriptor(id = "s1"), first)
        runtime.connect(SessionDescriptor(id = "s2"), second)
        runtime.receive("s1", RealtimeFrame(ref = "1", event = RealtimeFrameEvents.JOIN, channel = "chat/general"))
        runtime.receive("s2", RealtimeFrame(ref = "2", event = RealtimeFrameEvents.JOIN, channel = "chat/general"))
        runtime.receive(
            "s1",
            RealtimeFrame(
                ref = "3",
                event = RealtimeFrameEvents.MESSAGE,
                channel = "chat/general",
                payload = ChatMessage("hello"),
            ),
        )

        assertTrue(first.sent.any { it.channel == "chat/general" && it.payload == ChatMessage("general:hello") })
        assertTrue(second.sent.any { it.channel == "chat/general" && it.payload == ChatMessage("general:hello") })
        assertEquals(2, runtime.snapshot().maxObservedBroadcastFanOut)
    }

    @Test
    fun `rejects messages before join`() {
        val app =
            realtime {
                channel<ChatMessage>("chat/{roomId}") {
                    onMessage { message -> broadcast(message) }
                }
            }
        val runtime = LocalRealtimeRuntime(app, InMemoryPresenceStore())
        val transport = RecordingTransportSession()

        runtime.connect(SessionDescriptor(id = "s1"), transport)
        runtime.receive(
            "s1",
            RealtimeFrame(
                ref = "1",
                event = RealtimeFrameEvents.MESSAGE,
                channel = "chat/general",
                payload = ChatMessage("hello"),
            ),
        )

        assertTrue(
            transport.sent.any {
                it.event == RealtimeFrameEvents.ERROR &&
                    it.payload == RealtimeErrorPayload(
                        code = RealtimeErrorCodes.NOT_JOINED,
                        message = "Session has not joined 'chat/general'",
                    )
            },
        )
    }

    @Test
    fun `disconnect invokes leave handlers and clears memberships`() {
        val app =
            realtime {
                channel<ChatMessage>("chat/{roomId}") {
                    onJoin { presence.track(session.id) }
                    onLeave { presence.untrack(session.id) }
                }
            }
        val runtime = LocalRealtimeRuntime(app, InMemoryPresenceStore())
        val transport = RecordingTransportSession()

        runtime.connect(SessionDescriptor(id = "s1"), transport)
        runtime.receive("s1", RealtimeFrame(ref = "1", event = RealtimeFrameEvents.JOIN, channel = "chat/general"))
        runtime.disconnect("s1")

        assertEquals(0, runtime.snapshot().activeSessions)
        assertEquals(0, runtime.snapshot().activeMemberships)
        assertEquals("closed", transport.closedReason)
        assertEquals(1, runtime.snapshot().disconnectsByReason.getValue("closed"))
    }

    @Test
    fun `heartbeat cycle sends heartbeat frames to active sessions`() {
        val app =
            realtime {
                channel<ChatMessage>("chat/{roomId}") {
                }
            }
        val runtime =
            LocalRealtimeRuntime(
                application = app,
                presenceStore = InMemoryPresenceStore(),
                options =
                    RealtimeRuntimeOptions(
                        heartbeatInterval = 30.seconds.toJavaDuration(),
                        heartbeatTimeout = 90.seconds.toJavaDuration(),
                    ),
            )
        val transport = RecordingTransportSession()

        runtime.connect(SessionDescriptor(id = "s1"), transport)
        runtime.runHeartbeatCycle()

        val heartbeat = transport.sent.singleOrNull { it.event == RealtimeFrameEvents.HEARTBEAT }
        assertNotNull(heartbeat)
        assertEquals(mapOf("intervalMillis" to 30_000L, "timeoutMillis" to 90_000L), heartbeat.payload)

        runtime.close()
    }

    @Test
    fun `heartbeat cycle closes stale sessions`() {
        var now = 0L
        val app =
            realtime {
                channel<ChatMessage>("chat/{roomId}") {
                    onJoin { presence.track(session.id) }
                    onLeave { presence.untrack(session.id) }
                }
            }
        val runtime =
            LocalRealtimeRuntime(
                application = app,
                presenceStore = InMemoryPresenceStore(),
                options =
                    RealtimeRuntimeOptions(
                        heartbeatInterval = 30.seconds.toJavaDuration(),
                        heartbeatTimeout = 90.seconds.toJavaDuration(),
                    ),
                nanoTime = { now },
            )
        val transport = RecordingTransportSession()

        runtime.connect(SessionDescriptor(id = "s1"), transport)
        runtime.receive("s1", RealtimeFrame(ref = "1", event = RealtimeFrameEvents.JOIN, channel = "chat/general"))

        now = 91.seconds.inWholeNanoseconds
        runtime.runHeartbeatCycle(now)

        assertEquals(0, runtime.snapshot().activeSessions)
        assertEquals(0, runtime.snapshot().activeMemberships)
        assertEquals(1, runtime.snapshot().heartbeatTimeouts)
        assertEquals("heartbeat timeout", transport.closedReason)
        assertEquals(1, runtime.snapshot().disconnectsByReason.getValue("heartbeat_timeout"))

        runtime.close()
    }

    @Test
    fun `close disconnects all sessions and clears memberships`() {
        val app =
            realtime {
                channel<ChatMessage>("chat/{roomId}") {
                    onJoin { presence.track(session.id) }
                    onLeave { presence.untrack(session.id) }
                }
            }
        val runtime = LocalRealtimeRuntime(app, InMemoryPresenceStore())
        val first = RecordingTransportSession()
        val second = RecordingTransportSession()

        runtime.connect(SessionDescriptor(id = "s1"), first)
        runtime.connect(SessionDescriptor(id = "s2"), second)
        runtime.receive("s1", RealtimeFrame(ref = "1", event = RealtimeFrameEvents.JOIN, channel = "chat/general"))
        runtime.receive("s2", RealtimeFrame(ref = "2", event = RealtimeFrameEvents.JOIN, channel = "chat/general"))

        runtime.close()

        assertEquals(0, runtime.snapshot().activeSessions)
        assertEquals(0, runtime.snapshot().activeMemberships)
        assertEquals("runtime closed", first.closedReason)
        assertEquals("runtime closed", second.closedReason)
        assertEquals(2, runtime.snapshot().disconnectsByReason.getValue("runtime_closed"))
    }

    @Test
    fun `can execute handlers on virtual threads`() {
        val handlerRan = CountDownLatch(1)
        val handlerWasVirtual = AtomicBoolean(false)
        val app =
            realtime {
                channel<ChatMessage>("chat/{roomId}") {
                    onMessage {
                        handlerWasVirtual.set(Thread.currentThread().isVirtual)
                        handlerRan.countDown()
                    }
                }
            }
        val runtime =
            LocalRealtimeRuntime(
                application = app,
                presenceStore = InMemoryPresenceStore(),
                options =
                    RealtimeRuntimeOptions(
                        handlerExecution = RuntimeHandlerExecution.VIRTUAL_THREADS,
                        virtualThreadNamePrefix = "kanal-test-handler",
                    ),
            )
        val transport = RecordingTransportSession()

        try {
            runtime.connect(SessionDescriptor(id = "s1"), transport)
            runtime.receive("s1", RealtimeFrame(ref = "1", event = RealtimeFrameEvents.JOIN, channel = "chat/general"))
            runtime.receive(
                "s1",
                RealtimeFrame(
                    ref = "2",
                    event = RealtimeFrameEvents.MESSAGE,
                    channel = "chat/general",
                    payload = ChatMessage("hello"),
                ),
            )

            assertTrue(handlerRan.await(3, TimeUnit.SECONDS))
            assertTrue(handlerWasVirtual.get())
        } finally {
            runtime.close()
        }
    }

    @Test
    fun `records handler failures without throwing from receive`() {
        val app =
            realtime {
                channel<ChatMessage>("chat/{roomId}") {
                    onMessage {
                        error("boom")
                    }
                }
            }
        val runtime = LocalRealtimeRuntime(app, InMemoryPresenceStore())
        val transport = RecordingTransportSession()

        runtime.connect(SessionDescriptor(id = "s1"), transport)
        runtime.receive("s1", RealtimeFrame(ref = "1", event = RealtimeFrameEvents.JOIN, channel = "chat/general"))
        runtime.receive(
            "s1",
            RealtimeFrame(
                ref = "2",
                event = RealtimeFrameEvents.MESSAGE,
                channel = "chat/general",
                payload = ChatMessage("hello"),
            ),
        )

        assertEquals(1, runtime.snapshot().handlerFailures)
        assertEquals(1, runtime.snapshot().activeSessions)

        runtime.close()
    }

    @Test
    fun `turns payload decode failures into error frames`() {
        val app =
            realtime {
                channel<ChatMessage>("chat/{roomId}") {
                    onMessage { message -> broadcast(message) }
                }
            }
        val runtime =
            LocalRealtimeRuntime(
                application = app,
                presenceStore = InMemoryPresenceStore(),
                codec =
                    object : RuntimePayloadCodec {
                        override fun <T : Any> decode(
                            payload: Any?,
                            type: kotlin.reflect.KClass<T>,
                        ): T {
                            error("bad payload")
                        }

                        override fun encode(message: Any): Any = message
                    },
            )
        val transport = RecordingTransportSession()

        runtime.connect(SessionDescriptor(id = "s1"), transport)
        runtime.receive("s1", RealtimeFrame(ref = "1", event = RealtimeFrameEvents.JOIN, channel = "chat/general"))
        runtime.receive(
            "s1",
            RealtimeFrame(
                ref = "2",
                event = RealtimeFrameEvents.MESSAGE,
                channel = "chat/general",
                payload = mapOf("body" to "hello"),
            ),
        )

        assertEquals(1, runtime.snapshot().payloadDecodeFailures)
        assertTrue(
            transport.sent.any {
                it.event == RealtimeFrameEvents.ERROR &&
                    it.ref == "2" &&
                    it.payload == RealtimeErrorPayload(
                        code = RealtimeErrorCodes.PAYLOAD_DECODE_FAILED,
                        message = "Payload could not be decoded for 'chat/general'",
                    )
            },
        )

        runtime.close()
    }

    @Test
    fun `diagnostics include session channels and queue depths`() {
        var now = 0L
        val app =
            realtime {
                channel<ChatMessage>("chat/{roomId}") {
                }
            }
        val runtime =
            LocalRealtimeRuntime(
                application = app,
                presenceStore = InMemoryPresenceStore(),
                nanoTime = { now },
            )
        val transport = RecordingTransportSession()

        runtime.connect(SessionDescriptor(id = "s1", userId = "u1"), transport)
        runtime.receive("s1", RealtimeFrame(ref = "1", event = RealtimeFrameEvents.JOIN, channel = "chat/general"))
        now = 1.seconds.inWholeNanoseconds

        val diagnostics = runtime.diagnostics(now)
        val session = diagnostics.sessions.single()

        assertEquals("s1", session.sessionId)
        assertEquals("u1", session.userId)
        assertEquals(listOf("chat/general"), session.channels)
        assertEquals(1_000L, session.lastSeenAgeMillis)
        assertTrue(session.queueDepths.any { it.policy == BackpressurePolicy.DROP_OLDEST && it.capacity == 256 })

        runtime.close()
    }

    private class RecordingTransportSession : RuntimeTransportSession {
        val sent = mutableListOf<RealtimeFrame>()
        var closedReason: String? = null

        override fun send(frame: RealtimeFrame) {
            sent += frame
        }

        override fun close(reason: String) {
            closedReason = reason
        }
    }
}
