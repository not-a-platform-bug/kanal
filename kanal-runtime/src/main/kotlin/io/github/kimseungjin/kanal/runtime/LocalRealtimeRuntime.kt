package io.github.kimseungjin.kanal.runtime

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import io.github.kimseungjin.kanal.core.ChannelAddress
import io.github.kimseungjin.kanal.core.ChannelContext
import io.github.kimseungjin.kanal.core.ChannelDefinition
import io.github.kimseungjin.kanal.core.OutboundPublisher
import io.github.kimseungjin.kanal.core.PresenceStore
import io.github.kimseungjin.kanal.core.RealtimeApplication
import io.github.kimseungjin.kanal.core.SessionDescriptor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class LocalRealtimeRuntime(
    private val application: RealtimeApplication,
    private val presenceStore: PresenceStore,
    private val codec: RuntimePayloadCodec = IdentityRuntimePayloadCodec,
    private val options: RealtimeRuntimeOptions = RealtimeRuntimeOptions(),
    private val metrics: RuntimeMetrics = RuntimeMetrics(),
    private val handlerExecutor: RuntimeHandlerExecutor = options.handlerExecution.createExecutor(options.virtualThreadNamePrefix),
    private val nanoTime: () -> Long = System::nanoTime,
) : AutoCloseable {
    private val resolver = MeasuredChannelResolver(application, metrics)
    private val memberships = LocalMembershipIndex(metrics)
    private val sessions = ConcurrentHashMap<String, RuntimeSessionState>()
    private val closed = AtomicBoolean(false)
    private val heartbeatScheduler: ScheduledExecutorService? = createHeartbeatScheduler()

    init {
        scheduleHeartbeat()
    }

    fun connect(
        session: SessionDescriptor,
        transport: RuntimeTransportSession,
    ) {
        check(!closed.get()) { "Runtime is closed" }

        val existing =
            sessions.putIfAbsent(
                session.id,
                RuntimeSessionState(session, transport, options.outboundQueueCapacity, metrics, nanoTime()),
            )

        require(existing == null) { "Session '${session.id}' is already connected" }
        metrics.sessionOpened()
    }

    fun receive(
        sessionId: String,
        frame: RealtimeFrame,
    ) {
        if (closed.get()) {
            return
        }

        val session = sessions[sessionId] ?: return
        session.markSeen(nanoTime())
        metrics.recordInboundFrame()

        when (frame.event) {
            RealtimeFrameEvents.JOIN -> join(session, frame)
            RealtimeFrameEvents.LEAVE -> leave(session, frame)
            RealtimeFrameEvents.MESSAGE -> message(session, frame)
            RealtimeFrameEvents.HEARTBEAT -> reply(session, frame, RealtimeFrameEvents.HEARTBEAT, emptyMap<String, String>())
            else -> error(session, frame, RealtimeErrorCodes.UNKNOWN_EVENT, "Unknown event '${frame.event}'")
        }
    }

    fun disconnect(
        sessionId: String,
        reason: String = RuntimeDisconnectReasons.CLOSED,
    ) {
        val session = sessions[sessionId] ?: return

        memberships.channels(sessionId).forEach { address ->
            session.definition(address)?.let { definition ->
                invokeHandler {
                    definition.invokeLeave(context(session, address, definition))
                }
            }
        }

        memberships.removeSession(sessionId)
        sessions.remove(sessionId)
        metrics.sessionClosed()
        metrics.recordDisconnect(reason)
        session.transport.close(reason)
    }

    fun snapshot(): RuntimeMetricsSnapshot = metrics.snapshot()

    fun diagnostics(nowNanos: Long = nanoTime()): RuntimeDiagnosticsSnapshot =
        RuntimeDiagnosticsSnapshot(
            metrics = snapshot(),
            sessions =
                sessions.values
                    .map { it.diagnostics(nowNanos) }
                    .sortedBy { it.sessionId },
        )

    internal fun runHeartbeatCycle(nowNanos: Long = nanoTime()) {
        if (closed.get()) {
            return
        }

        val timeoutNanos = options.heartbeatTimeout.toNanos()
        sessions.values.forEach { session ->
            if (nowNanos - session.lastSeenNanos() > timeoutNanos) {
                metrics.recordHeartbeatTimeout()
                disconnect(session.descriptor.id, RuntimeDisconnectReasons.HEARTBEAT_TIMEOUT)
            } else {
                sendHeartbeat(session)
            }
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }

        heartbeatScheduler?.shutdownNow()
        sessions.keys.toList().forEach { sessionId ->
            disconnect(sessionId, RuntimeDisconnectReasons.RUNTIME_CLOSED)
        }
        handlerExecutor.close()
    }

    private fun join(
        session: RuntimeSessionState,
        frame: RealtimeFrame,
    ) {
        val resolution =
            resolve(frame, session)
                ?: return

        memberships.join(session.descriptor, resolution.address)
        session.track(resolution.address, resolution.definition)

        invokeHandler {
            resolution.definition.invokeJoin(context(session, resolution.address, resolution.definition))
        }

        reply(session, frame, RealtimeFrameEvents.JOIN, emptyMap<String, String>())
    }

    private fun leave(
        session: RuntimeSessionState,
        frame: RealtimeFrame,
    ) {
        val resolution =
            resolve(frame, session)
                ?: return

        invokeHandler {
            resolution.definition.invokeLeave(context(session, resolution.address, resolution.definition))
        }

        memberships.leave(session.descriptor.id, resolution.address)
        session.untrack(resolution.address)
        reply(session, frame, RealtimeFrameEvents.LEAVE, emptyMap<String, String>())
    }

    private fun message(
        session: RuntimeSessionState,
        frame: RealtimeFrame,
    ) {
        val resolution =
            resolve(frame, session)
                ?: return

        if (!memberships.contains(session.descriptor.id, resolution.address)) {
            error(session, frame, RealtimeErrorCodes.NOT_JOINED, "Session has not joined '${frame.channel}'")
            return
        }

        val message =
            try {
                codec.decode(frame.payload, resolution.definition.messageType)
            } catch (_: Throwable) {
                metrics.recordPayloadDecodeFailure()
                error(session, frame, RealtimeErrorCodes.PAYLOAD_DECODE_FAILED, "Payload could not be decoded for '${frame.channel}'")
                return
            }

        invokeHandler {
            resolution.definition.invokeMessage(
                context(session, resolution.address, resolution.definition),
                message,
            )
        }
    }

    private fun resolve(
        frame: RealtimeFrame,
        session: RuntimeSessionState,
    ): io.github.kimseungjin.kanal.core.ChannelResolution? {
        val channel = frame.channel
        if (channel.isNullOrBlank()) {
            error(session, frame, RealtimeErrorCodes.MISSING_CHANNEL, "Frame channel must not be blank")
            return null
        }

        val resolution = resolver.resolve(channel)
        if (resolution == null) {
            error(session, frame, RealtimeErrorCodes.CHANNEL_NOT_FOUND, "No channel registered for '$channel'")
            return null
        }

        return resolution
    }

    private fun context(
        session: RuntimeSessionState,
        address: ChannelAddress,
        definition: ChannelDefinition<*>,
    ): ChannelContext =
        ChannelContext(
            session = session.descriptor,
            address = address,
            publisher = RuntimeOutboundPublisher(definition.backpressurePolicy),
            store = presenceStore,
        )

    private fun invokeHandler(block: () -> Unit) {
        handlerExecutor.execute {
            val startedAt = System.nanoTime()
            try {
                block()
            } catch (_: Throwable) {
                metrics.recordHandlerFailure()
            } finally {
                metrics.recordHandlerLatency(System.nanoTime() - startedAt)
            }
        }
    }

    private fun reply(
        session: RuntimeSessionState,
        request: RealtimeFrame,
        event: String,
        payload: Any,
    ) {
        session.enqueue(
            frame =
                RealtimeFrame(
                    ref = request.ref,
                    event = RealtimeFrameEvents.REPLY,
                    channel = request.channel,
                    payload = RealtimeReplyPayload(event = event, response = payload),
                ),
            policy = BackpressurePolicy.DROP_OLDEST,
        )
    }

    private fun sendHeartbeat(session: RuntimeSessionState) {
        session.enqueue(
            frame =
                RealtimeFrame(
                    event = RealtimeFrameEvents.HEARTBEAT,
                    payload =
                        mapOf(
                            "intervalMillis" to options.heartbeatInterval.toMillis(),
                            "timeoutMillis" to options.heartbeatTimeout.toMillis(),
                        ),
                ),
            policy = BackpressurePolicy.DROP_OLDEST,
        )
    }

    private fun error(
        session: RuntimeSessionState,
        request: RealtimeFrame,
        code: String,
        reason: String,
    ) {
        session.enqueue(
            frame =
                RealtimeFrame(
                    ref = request.ref,
                    event = RealtimeFrameEvents.ERROR,
                    channel = request.channel,
                    payload = RealtimeErrorPayload(code = code, message = reason),
                ),
            policy = BackpressurePolicy.DROP_OLDEST,
        )
    }

    private fun ChannelAddress.concretePath(): String =
        pattern.segments.joinToString("/") { segment ->
            when (segment) {
                is io.github.kimseungjin.kanal.core.ChannelPatternSegment.Static -> segment.value
                is io.github.kimseungjin.kanal.core.ChannelPatternSegment.Parameter -> parameters.getValue(segment.name)
            }
        }

    private fun createHeartbeatScheduler(): ScheduledExecutorService? {
        if (options.heartbeatInterval.isZero) {
            return null
        }

        return Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "kanal-heartbeat").apply {
                isDaemon = true
            }
        }
    }

    private fun scheduleHeartbeat() {
        val interval = options.heartbeatInterval
        val scheduler = heartbeatScheduler ?: return

        scheduler.scheduleWithFixedDelay(
            { runCatching { runHeartbeatCycle() } },
            interval.toNanos(),
            interval.toNanos(),
            TimeUnit.NANOSECONDS,
        )
    }

    private inner class RuntimeOutboundPublisher(
        private val policy: BackpressurePolicy,
    ) : OutboundPublisher {
        override fun broadcast(
            address: ChannelAddress,
            message: Any,
        ) {
            val payload = codec.encode(message)
            val channel = address.concretePath()
            memberships.forEachBroadcastTarget(address) { sessionId ->
                val result =
                    sessions[sessionId]?.enqueue(
                        frame =
                            RealtimeFrame(
                                event = RealtimeFrameEvents.MESSAGE,
                                channel = channel,
                                payload = payload,
                            ),
                        policy = policy,
                    )

                if (result == OutboundQueueOfferResult.DISCONNECT) {
                    disconnect(sessionId, RuntimeDisconnectReasons.BACKPRESSURE)
                }
            }
        }

        override fun sendToSession(
            session: SessionDescriptor,
            message: Any,
        ) {
            val result =
                sessions[session.id]?.enqueue(
                    frame =
                        RealtimeFrame(
                            event = RealtimeFrameEvents.MESSAGE,
                            payload = codec.encode(message),
                        ),
                    policy = policy,
                )

            if (result == OutboundQueueOfferResult.DISCONNECT) {
                disconnect(session.id, RuntimeDisconnectReasons.BACKPRESSURE)
            }
        }
    }

    private inner class RuntimeSessionState(
        val descriptor: SessionDescriptor,
        val transport: RuntimeTransportSession,
        queueCapacity: Int,
        metrics: RuntimeMetrics,
        openedAtNanos: Long,
    ) {
        private val queues = ConcurrentHashMap<BackpressurePolicy, BoundedOutboundQueue<RealtimeFrame>>()
        private val definitions = ConcurrentHashMap<ChannelAddress, ChannelDefinition<*>>()
        private val draining = AtomicBoolean(false)
        private val lastSeenNanos = AtomicLong(openedAtNanos)
        private val queueFactory = { policy: BackpressurePolicy ->
            BoundedOutboundQueue<RealtimeFrame>(queueCapacity, policy, metrics)
        }

        fun markSeen(nowNanos: Long) {
            lastSeenNanos.set(nowNanos)
        }

        fun lastSeenNanos(): Long = lastSeenNanos.get()

        fun track(
            address: ChannelAddress,
            definition: ChannelDefinition<*>,
        ) {
            definitions[address] = definition
        }

        fun untrack(address: ChannelAddress) {
            definitions.remove(address)
        }

        fun definition(address: ChannelAddress): ChannelDefinition<*>? = definitions[address]

        fun diagnostics(nowNanos: Long): RuntimeSessionDiagnostics =
            RuntimeSessionDiagnostics(
                sessionId = descriptor.id,
                userId = descriptor.userId,
                channels =
                    definitions.keys
                        .map { it.concretePath() }
                        .sorted(),
                queueDepths =
                    queues.values
                        .map {
                            RuntimeQueueDiagnostics(
                                policy = it.policy,
                                depth = it.size(),
                                capacity = it.capacity,
                            )
                        }
                        .sortedBy { it.policy.name },
                lastSeenAgeMillis = (nowNanos - lastSeenNanos()).coerceAtLeast(0) / 1_000_000,
            )

        fun enqueue(
            frame: RealtimeFrame,
            policy: BackpressurePolicy,
        ): OutboundQueueOfferResult {
            val queue = queues.computeIfAbsent(policy, queueFactory)
            val result = queue.offer(frame)

            when (result) {
                OutboundQueueOfferResult.DISCONNECT,
                OutboundQueueOfferResult.WOULD_SUSPEND -> return result
                else -> drain()
            }

            return result
        }

        private fun drain() {
            if (!draining.compareAndSet(false, true)) {
                return
            }

            try {
                var sent: Int
                do {
                    sent = 0
                    queues.values.forEach { queue ->
                        while (true) {
                            val frame = queue.poll() ?: break
                            transport.send(frame)
                            sent += 1
                        }
                    }
                } while (sent > 0)
            } finally {
                draining.set(false)
            }

            if (queues.values.any { it.size() > 0 }) {
                drain()
            }
        }
    }
}

data class RuntimeDiagnosticsSnapshot(
    val metrics: RuntimeMetricsSnapshot,
    val sessions: List<RuntimeSessionDiagnostics>,
)

data class RuntimeSessionDiagnostics(
    val sessionId: String,
    val userId: String?,
    val channels: List<String>,
    val queueDepths: List<RuntimeQueueDiagnostics>,
    val lastSeenAgeMillis: Long,
)

data class RuntimeQueueDiagnostics(
    val policy: BackpressurePolicy,
    val depth: Int,
    val capacity: Int,
)
