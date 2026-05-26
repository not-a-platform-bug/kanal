package io.github.kimseungjin.kanal.runtime

import io.github.kimseungjin.kanal.core.BackpressurePolicy

class BoundedOutboundQueue<T : Any>(
    val capacity: Int,
    val policy: BackpressurePolicy,
    private val metrics: RuntimeMetrics? = null,
) {
    private val lock = Any()
    private val items = ArrayDeque<T>(capacity)

    init {
        require(capacity > 0) { "Outbound queue capacity must be greater than zero" }
    }

    fun offer(item: T): OutboundQueueOfferResult =
        synchronized(lock) {
            if (items.size < capacity) {
                items.addLast(item)
                metrics?.recordOutboundFrame()
                metrics?.recordOutboundQueueDepth(items.size)
                return@synchronized OutboundQueueOfferResult.ENQUEUED
            }

            when (policy) {
                BackpressurePolicy.SUSPEND -> {
                    metrics?.recordSlowConsumerSignal(policy)
                    metrics?.recordOutboundQueueDepth(items.size)
                    OutboundQueueOfferResult.WOULD_SUSPEND
                }

                BackpressurePolicy.DROP_OLDEST -> {
                    items.removeFirst()
                    items.addLast(item)
                    metrics?.recordOutboundFrame()
                    metrics?.recordSlowConsumerSignal(policy)
                    metrics?.recordDroppedOutboundMessage(policy)
                    metrics?.recordOutboundQueueDepth(items.size)
                    OutboundQueueOfferResult.DROPPED_OLDEST
                }

                BackpressurePolicy.DROP_LATEST -> {
                    metrics?.recordSlowConsumerSignal(policy)
                    metrics?.recordDroppedOutboundMessage(policy)
                    metrics?.recordOutboundQueueDepth(items.size)
                    OutboundQueueOfferResult.DROPPED_LATEST
                }

                BackpressurePolicy.DISCONNECT -> {
                    metrics?.recordSlowConsumerSignal(policy)
                    metrics?.recordDisconnect(policy)
                    metrics?.recordOutboundQueueDepth(items.size)
                    OutboundQueueOfferResult.DISCONNECT
                }
            }
        }

    fun poll(): T? =
        synchronized(lock) {
            val item = items.removeFirstOrNull()
            metrics?.recordOutboundQueueDepth(items.size)
            item
        }

    fun size(): Int = synchronized(lock) { items.size }

    fun snapshot(): List<T> = synchronized(lock) { items.toList() }
}

enum class OutboundQueueOfferResult {
    ENQUEUED,
    WOULD_SUSPEND,
    DROPPED_OLDEST,
    DROPPED_LATEST,
    DISCONNECT,
}
