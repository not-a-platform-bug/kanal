package io.github.kimseungjin.kanal.spring

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import io.github.kimseungjin.kanal.core.InMemoryPresenceStore
import io.github.kimseungjin.kanal.core.SessionDescriptor
import io.github.kimseungjin.kanal.core.dsl.channel
import io.github.kimseungjin.kanal.core.dsl.realtime
import io.github.kimseungjin.kanal.runtime.LocalRealtimeRuntime
import io.github.kimseungjin.kanal.runtime.RealtimeFrame
import io.github.kimseungjin.kanal.runtime.RealtimeFrameEvents
import io.github.kimseungjin.kanal.runtime.RuntimeMetrics
import io.github.kimseungjin.kanal.runtime.RuntimeTransportSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KanalRuntimeEndpointTest {
    @Test
    fun `returns a plain runtime diagnostics snapshot`() {
        val metrics = RuntimeMetrics()

        metrics.recordDroppedOutboundMessage(BackpressurePolicy.DROP_OLDEST)
        metrics.recordSlowConsumerSignal(BackpressurePolicy.DROP_OLDEST)
        metrics.recordDisconnect("runtime closed")
        metrics.recordHeartbeatTimeout()
        metrics.recordHandlerLatency(100)
        metrics.recordHandlerLatency(300)
        metrics.recordHandlerFailure()
        metrics.recordPayloadDecodeFailure()
        val runtime =
            LocalRealtimeRuntime(
                application =
                    realtime {
                        channel<Message>("chat/{roomId}") {
                            onMessage { message -> broadcast(message) }
                        }
                    },
                presenceStore = InMemoryPresenceStore(),
                metrics = metrics,
            )
        val transport = RecordingTransportSession()

        runtime.connect(SessionDescriptor(id = "s1", userId = "u1"), transport)
        runtime.receive("s1", RealtimeFrame(event = RealtimeFrameEvents.JOIN, channel = "chat/general"))
        runtime.receive(
            "s1",
            RealtimeFrame(
                event = RealtimeFrameEvents.MESSAGE,
                channel = "chat/general",
                payload = Message("hello"),
            ),
        )

        val response = KanalRuntimeEndpoint(runtime).runtime()

        assertEquals(1, response.metrics.activeSessions)
        assertEquals(1, response.metrics.activeMemberships)
        assertEquals(2, response.metrics.inboundFrames)
        assertEquals(1, response.metrics.droppedOutboundMessages)
        assertEquals(1, response.metrics.slowConsumerSignals)
        assertEquals(1, response.metrics.dropsByPolicy.getValue("drop_oldest"))
        assertEquals(1, response.metrics.slowConsumerSignalsByPolicy.getValue("drop_oldest"))
        assertEquals(1, response.metrics.disconnectsByReason.getValue("runtime_closed"))
        assertEquals(1, response.metrics.heartbeatTimeouts)
        assertEquals(1, response.metrics.handlerFailures)
        assertEquals(1, response.metrics.payloadDecodeFailures)
        assertTrue(response.metrics.averageHandlerLatencyNanos > 0)
        assertTrue(response.metrics.maxHandlerLatencyNanos >= 300)
        assertEquals("s1", response.sessions.single().sessionId)
        assertEquals("u1", response.sessions.single().userId)
        assertEquals(listOf("chat/general"), response.sessions.single().channels)
        assertEquals(true, response.sessions.single().queueDepths.any { it.policy == "suspend" })
        assertTrue(response.events.any { it.type == "session_connected" && it.sessionId == "s1" })
        assertTrue(response.events.any { it.type == "joined" && it.channel == "chat/general" })
    }

    data class Message(
        val body: String,
    )

    private class RecordingTransportSession : RuntimeTransportSession {
        override fun send(frame: RealtimeFrame) {
        }
    }
}
