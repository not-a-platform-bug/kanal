package io.github.kimseungjin.kanal.spring

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import io.github.kimseungjin.kanal.runtime.LocalRealtimeRuntime
import io.github.kimseungjin.kanal.runtime.RuntimeDiagnosticsSnapshot
import io.github.kimseungjin.kanal.runtime.RuntimeMetricsSnapshot
import io.github.kimseungjin.kanal.runtime.RuntimeQueueDiagnostics
import io.github.kimseungjin.kanal.runtime.RuntimeSessionDiagnostics
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation

@Endpoint(id = "kanal")
class KanalRuntimeEndpoint(
    private val runtime: LocalRealtimeRuntime,
) {
    @ReadOperation
    fun runtime(): KanalRuntimeEndpointResponse =
        runtime.diagnostics().toResponse()

    private fun RuntimeDiagnosticsSnapshot.toResponse(): KanalRuntimeEndpointResponse =
        KanalRuntimeEndpointResponse(
            metrics = metrics.toResponse(),
            sessions = sessions.map { it.toResponse() },
        )

    private fun RuntimeMetricsSnapshot.toResponse(): KanalRuntimeMetricsResponse =
        KanalRuntimeMetricsResponse(
            activeSessions = activeSessions,
            activeMemberships = activeMemberships,
            inboundFrames = inboundFrames,
            outboundFrames = outboundFrames,
            droppedOutboundMessages = droppedOutboundMessages,
            slowConsumerSignals = slowConsumerSignals,
            dropsByPolicy = dropsByPolicy.toStringKeyMap(),
            slowConsumerSignalsByPolicy = slowConsumerSignalsByPolicy.toStringKeyMap(),
            disconnectsByPolicy = disconnectsByPolicy.toStringKeyMap(),
            disconnectsByReason = disconnectsByReason,
            heartbeatTimeouts = heartbeatTimeouts,
            channelResolutionCount = channelResolutionCount,
            averageChannelResolutionNanos = averageChannelResolutionNanos,
            handlerLatencyCount = handlerLatencyCount,
            handlerFailures = handlerFailures,
            payloadDecodeFailures = payloadDecodeFailures,
            averageHandlerLatencyNanos = averageHandlerLatencyNanos,
            maxHandlerLatencyNanos = maxHandlerLatencyNanos,
            maxObservedOutboundQueueDepth = maxObservedOutboundQueueDepth,
            maxObservedBroadcastFanOut = maxObservedBroadcastFanOut,
        )

    private fun RuntimeSessionDiagnostics.toResponse(): KanalRuntimeSessionResponse =
        KanalRuntimeSessionResponse(
            sessionId = sessionId,
            userId = userId,
            channels = channels,
            queueDepths = queueDepths.map { it.toResponse() },
            lastSeenAgeMillis = lastSeenAgeMillis,
        )

    private fun RuntimeQueueDiagnostics.toResponse(): KanalRuntimeQueueResponse =
        KanalRuntimeQueueResponse(
            policy = policy.name.lowercase(),
            depth = depth,
            capacity = capacity,
        )

    private fun Map<BackpressurePolicy, Long>.toStringKeyMap(): Map<String, Long> =
        mapKeys { it.key.name.lowercase() }
}

data class KanalRuntimeEndpointResponse(
    val metrics: KanalRuntimeMetricsResponse,
    val sessions: List<KanalRuntimeSessionResponse>,
)

data class KanalRuntimeMetricsResponse(
    val activeSessions: Int,
    val activeMemberships: Int,
    val inboundFrames: Long,
    val outboundFrames: Long,
    val droppedOutboundMessages: Long,
    val slowConsumerSignals: Long,
    val dropsByPolicy: Map<String, Long>,
    val slowConsumerSignalsByPolicy: Map<String, Long>,
    val disconnectsByPolicy: Map<String, Long>,
    val disconnectsByReason: Map<String, Long>,
    val heartbeatTimeouts: Long,
    val channelResolutionCount: Long,
    val averageChannelResolutionNanos: Long,
    val handlerLatencyCount: Long,
    val handlerFailures: Long,
    val payloadDecodeFailures: Long,
    val averageHandlerLatencyNanos: Long,
    val maxHandlerLatencyNanos: Long,
    val maxObservedOutboundQueueDepth: Int,
    val maxObservedBroadcastFanOut: Int,
)

data class KanalRuntimeSessionResponse(
    val sessionId: String,
    val userId: String?,
    val channels: List<String>,
    val queueDepths: List<KanalRuntimeQueueResponse>,
    val lastSeenAgeMillis: Long,
)

data class KanalRuntimeQueueResponse(
    val policy: String,
    val depth: Int,
    val capacity: Int,
)
