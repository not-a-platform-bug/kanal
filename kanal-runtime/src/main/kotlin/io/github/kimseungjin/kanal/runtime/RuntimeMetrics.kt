package io.github.kimseungjin.kanal.runtime

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import java.util.EnumMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder

class RuntimeMetrics {
    private val activeSessions = AtomicInteger()
    private val activeMemberships = AtomicInteger()
    private val inboundFrames = LongAdder()
    private val outboundFrames = LongAdder()
    private val droppedOutboundMessages = LongAdder()
    private val heartbeatTimeouts = LongAdder()
    private val channelResolutionCount = LongAdder()
    private val channelResolutionNanos = LongAdder()
    private val maxObservedOutboundQueueDepth = AtomicInteger()
    private val maxObservedBroadcastFanOut = AtomicInteger()
    private val disconnectsByPolicy =
        EnumMap<BackpressurePolicy, LongAdder>(BackpressurePolicy::class.java).apply {
            BackpressurePolicy.entries.forEach { put(it, LongAdder()) }
        }
    private val dropsByPolicy =
        EnumMap<BackpressurePolicy, LongAdder>(BackpressurePolicy::class.java).apply {
            BackpressurePolicy.entries.forEach { put(it, LongAdder()) }
        }
    private val handlerLatencyCount = LongAdder()
    private val handlerLatencyNanos = LongAdder()
    private val maxHandlerLatencyNanos = AtomicLong()

    fun sessionOpened() {
        activeSessions.incrementAndGet()
    }

    fun sessionClosed() {
        activeSessions.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
    }

    fun membershipJoined() {
        activeMemberships.incrementAndGet()
    }

    fun membershipLeft() {
        activeMemberships.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
    }

    fun recordInboundFrame() {
        inboundFrames.increment()
    }

    fun recordOutboundFrame() {
        outboundFrames.increment()
    }

    fun recordBroadcastFanOut(size: Int) {
        require(size >= 0) { "Broadcast fan-out size must not be negative" }
        maxObservedBroadcastFanOut.updateMax(size)
    }

    fun recordDroppedOutboundMessage(policy: BackpressurePolicy) {
        droppedOutboundMessages.increment()
        dropsByPolicy.getValue(policy).increment()
    }

    fun recordDisconnect(policy: BackpressurePolicy) {
        disconnectsByPolicy.getValue(policy).increment()
    }

    fun recordHeartbeatTimeout() {
        heartbeatTimeouts.increment()
    }

    fun recordChannelResolution(nanos: Long) {
        require(nanos >= 0) { "Channel resolution duration must not be negative" }
        channelResolutionCount.increment()
        channelResolutionNanos.add(nanos)
    }

    fun recordHandlerLatency(nanos: Long) {
        require(nanos >= 0) { "Handler latency must not be negative" }
        handlerLatencyCount.increment()
        handlerLatencyNanos.add(nanos)
        maxHandlerLatencyNanos.updateMax(nanos)
    }

    fun recordOutboundQueueDepth(depth: Int) {
        require(depth >= 0) { "Outbound queue depth must not be negative" }
        maxObservedOutboundQueueDepth.updateMax(depth)
    }

    fun snapshot(): RuntimeMetricsSnapshot =
        RuntimeMetricsSnapshot(
            activeSessions = activeSessions.get(),
            activeMemberships = activeMemberships.get(),
            inboundFrames = inboundFrames.sum(),
            outboundFrames = outboundFrames.sum(),
            droppedOutboundMessages = droppedOutboundMessages.sum(),
            dropsByPolicy = dropsByPolicy.mapValues { it.value.sum() },
            disconnectsByPolicy = disconnectsByPolicy.mapValues { it.value.sum() },
            heartbeatTimeouts = heartbeatTimeouts.sum(),
            channelResolutionCount = channelResolutionCount.sum(),
            channelResolutionNanos = channelResolutionNanos.sum(),
            handlerLatencyCount = handlerLatencyCount.sum(),
            handlerLatencyNanos = handlerLatencyNanos.sum(),
            maxHandlerLatencyNanos = maxHandlerLatencyNanos.get(),
            maxObservedOutboundQueueDepth = maxObservedOutboundQueueDepth.get(),
            maxObservedBroadcastFanOut = maxObservedBroadcastFanOut.get(),
        )

    private fun AtomicInteger.updateMax(candidate: Int) {
        updateAndGet { current -> maxOf(current, candidate) }
    }

    private fun AtomicLong.updateMax(candidate: Long) {
        updateAndGet { current -> maxOf(current, candidate) }
    }
}

data class RuntimeMetricsSnapshot(
    val activeSessions: Int,
    val activeMemberships: Int,
    val inboundFrames: Long,
    val outboundFrames: Long,
    val droppedOutboundMessages: Long,
    val dropsByPolicy: Map<BackpressurePolicy, Long>,
    val disconnectsByPolicy: Map<BackpressurePolicy, Long>,
    val heartbeatTimeouts: Long,
    val channelResolutionCount: Long,
    val channelResolutionNanos: Long,
    val handlerLatencyCount: Long,
    val handlerLatencyNanos: Long,
    val maxHandlerLatencyNanos: Long,
    val maxObservedOutboundQueueDepth: Int,
    val maxObservedBroadcastFanOut: Int,
) {
    val averageChannelResolutionNanos: Long =
        if (channelResolutionCount == 0L) 0L else channelResolutionNanos / channelResolutionCount

    val averageHandlerLatencyNanos: Long =
        if (handlerLatencyCount == 0L) 0L else handlerLatencyNanos / handlerLatencyCount
}
