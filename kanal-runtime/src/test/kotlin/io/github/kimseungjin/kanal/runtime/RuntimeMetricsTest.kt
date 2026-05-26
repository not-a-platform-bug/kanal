package io.github.kimseungjin.kanal.runtime

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class RuntimeMetricsTest {
    @Test
    fun `records runtime counters and latency summaries`() {
        val metrics = RuntimeMetrics()

        metrics.sessionOpened()
        metrics.membershipJoined()
        metrics.recordInboundFrame()
        metrics.recordOutboundFrame()
        metrics.recordBroadcastFanOut(12)
        metrics.recordDroppedOutboundMessage(BackpressurePolicy.DROP_LATEST)
        metrics.recordDisconnect(BackpressurePolicy.DISCONNECT)
        metrics.recordDisconnect("Heartbeat Timeout")
        metrics.recordSlowConsumerSignal(BackpressurePolicy.DROP_LATEST)
        metrics.recordHeartbeatTimeout()
        metrics.recordChannelResolution(10)
        metrics.recordChannelResolution(30)
        metrics.recordHandlerLatency(100)
        metrics.recordHandlerLatency(300)
        metrics.recordHandlerFailure()
        metrics.recordPayloadDecodeFailure()
        metrics.recordOutboundQueueDepth(7)

        val snapshot = metrics.snapshot()

        assertEquals(1, snapshot.activeSessions)
        assertEquals(1, snapshot.activeMemberships)
        assertEquals(1, snapshot.inboundFrames)
        assertEquals(1, snapshot.outboundFrames)
        assertEquals(1, snapshot.droppedOutboundMessages)
        assertEquals(1, snapshot.slowConsumerSignals)
        assertEquals(1, snapshot.dropsByPolicy.getValue(BackpressurePolicy.DROP_LATEST))
        assertEquals(1, snapshot.slowConsumerSignalsByPolicy.getValue(BackpressurePolicy.DROP_LATEST))
        assertEquals(1, snapshot.disconnectsByPolicy.getValue(BackpressurePolicy.DISCONNECT))
        assertEquals(1, snapshot.disconnectsByReason.getValue("heartbeat_timeout"))
        assertEquals(1, snapshot.heartbeatTimeouts)
        assertEquals(20, snapshot.averageChannelResolutionNanos)
        assertEquals(200, snapshot.averageHandlerLatencyNanos)
        assertEquals(1, snapshot.handlerFailures)
        assertEquals(1, snapshot.payloadDecodeFailures)
        assertEquals(300, snapshot.maxHandlerLatencyNanos)
        assertEquals(7, snapshot.maxObservedOutboundQueueDepth)
        assertEquals(12, snapshot.maxObservedBroadcastFanOut)
    }
}
