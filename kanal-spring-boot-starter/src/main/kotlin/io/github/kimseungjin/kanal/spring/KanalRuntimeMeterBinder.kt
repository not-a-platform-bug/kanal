package io.github.kimseungjin.kanal.spring

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import io.github.kimseungjin.kanal.runtime.RuntimeDisconnectReasons
import io.github.kimseungjin.kanal.runtime.RuntimeMetrics
import io.micrometer.core.instrument.FunctionCounter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder

class KanalRuntimeMeterBinder(
    private val metrics: RuntimeMetrics,
) : MeterBinder {
    override fun bindTo(registry: MeterRegistry) {
        gauge(registry, "kanal.runtime.sessions.active") { it.activeSessions.toDouble() }
        gauge(registry, "kanal.runtime.memberships.active") { it.activeMemberships.toDouble() }
        gauge(registry, "kanal.runtime.outbound.queue.depth.max") { it.maxObservedOutboundQueueDepth.toDouble() }
        gauge(registry, "kanal.runtime.broadcast.fanout.max") { it.maxObservedBroadcastFanOut.toDouble() }
        gauge(registry, "kanal.runtime.handler.latency.avg.nanos") { it.averageHandlerLatencyNanos.toDouble() }
        gauge(registry, "kanal.runtime.handler.latency.max.nanos") { it.maxHandlerLatencyNanos.toDouble() }
        gauge(registry, "kanal.runtime.channel.resolution.avg.nanos") { it.averageChannelResolutionNanos.toDouble() }

        counter(registry, "kanal.runtime.frames.inbound") { it.inboundFrames.toDouble() }
        counter(registry, "kanal.runtime.frames.outbound") { it.outboundFrames.toDouble() }
        counter(registry, "kanal.runtime.messages.dropped") { it.droppedOutboundMessages.toDouble() }
        counter(registry, "kanal.runtime.slow.consumers") { it.slowConsumerSignals.toDouble() }
        counter(registry, "kanal.runtime.heartbeat.timeouts") { it.heartbeatTimeouts.toDouble() }
        counter(registry, "kanal.runtime.handler.latency.count") { it.handlerLatencyCount.toDouble() }
        counter(registry, "kanal.runtime.handler.failures") { it.handlerFailures.toDouble() }
        counter(registry, "kanal.runtime.payload.decode.failures") { it.payloadDecodeFailures.toDouble() }
        counter(registry, "kanal.runtime.channel.resolution.count") { it.channelResolutionCount.toDouble() }

        BackpressurePolicy.entries.forEach { policy ->
            counter(registry, "kanal.runtime.messages.dropped", "policy", policy.tagValue()) {
                it.dropsByPolicy.getValue(policy).toDouble()
            }
            counter(registry, "kanal.runtime.disconnects", "policy", policy.tagValue()) {
                it.disconnectsByPolicy.getValue(policy).toDouble()
            }
            counter(registry, "kanal.runtime.slow.consumers", "policy", policy.tagValue()) {
                it.slowConsumerSignalsByPolicy.getValue(policy).toDouble()
            }
        }

        RuntimeDisconnectReasons.standard.forEach { reason ->
            val tag = reason.replace(' ', '_')
            counter(registry, "kanal.runtime.disconnects", "reason", tag) {
                it.disconnectsByReason[tag]?.toDouble() ?: 0.0
            }
        }
    }

    private fun gauge(
        registry: MeterRegistry,
        name: String,
        value: (io.github.kimseungjin.kanal.runtime.RuntimeMetricsSnapshot) -> Double,
    ) {
        Gauge
            .builder(name, metrics) { value(it.snapshot()) }
            .register(registry)
    }

    private fun counter(
        registry: MeterRegistry,
        name: String,
        value: (io.github.kimseungjin.kanal.runtime.RuntimeMetricsSnapshot) -> Double,
    ) {
        FunctionCounter
            .builder(name, metrics) { value(it.snapshot()) }
            .register(registry)
    }

    private fun counter(
        registry: MeterRegistry,
        name: String,
        tagKey: String,
        tagValue: String,
        value: (io.github.kimseungjin.kanal.runtime.RuntimeMetricsSnapshot) -> Double,
    ) {
        FunctionCounter
            .builder(name, metrics) { value(it.snapshot()) }
            .tag(tagKey, tagValue)
            .register(registry)
    }

    private fun BackpressurePolicy.tagValue(): String = name.lowercase()
}
