package io.github.kimseungjin.kanal.spring

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import io.github.kimseungjin.kanal.runtime.RuntimeMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class KanalRuntimeMeterBinderTest {
    @Test
    fun `binds runtime metrics to micrometer meters`() {
        val metrics = RuntimeMetrics()
        val registry = SimpleMeterRegistry()

        metrics.sessionOpened()
        metrics.membershipJoined()
        metrics.recordInboundFrame()
        metrics.recordOutboundFrame()
        metrics.recordDroppedOutboundMessage(BackpressurePolicy.DROP_LATEST)
        metrics.recordSlowConsumerSignal(BackpressurePolicy.DROP_LATEST)
        metrics.recordDisconnect("heartbeat timeout")
        metrics.recordHeartbeatTimeout()
        metrics.recordHandlerLatency(100)
        metrics.recordHandlerLatency(300)
        metrics.recordHandlerFailure()
        metrics.recordPayloadDecodeFailure()

        KanalRuntimeMeterBinder(metrics).bindTo(registry)

        assertEquals(1.0, registry.get("kanal.runtime.sessions.active").gauge().value())
        assertEquals(1.0, registry.get("kanal.runtime.memberships.active").gauge().value())
        assertEquals(1.0, registry.get("kanal.runtime.frames.inbound").functionCounter().count())
        assertEquals(1.0, registry.get("kanal.runtime.frames.outbound").functionCounter().count())
        assertEquals(1.0, registry.get("kanal.runtime.heartbeat.timeouts").functionCounter().count())
        assertEquals(1.0, registry.get("kanal.runtime.slow.consumers").functionCounter().count())
        assertEquals(1.0, registry.get("kanal.runtime.handler.failures").functionCounter().count())
        assertEquals(1.0, registry.get("kanal.runtime.payload.decode.failures").functionCounter().count())
        assertEquals(200.0, registry.get("kanal.runtime.handler.latency.avg.nanos").gauge().value())
        assertEquals(
            1.0,
            registry
                .get("kanal.runtime.messages.dropped")
                .tag("policy", "drop_latest")
                .functionCounter()
                .count(),
        )
        assertEquals(
            1.0,
            registry
                .get("kanal.runtime.disconnects")
                .tag("reason", "heartbeat_timeout")
                .functionCounter()
                .count(),
        )
    }
}
