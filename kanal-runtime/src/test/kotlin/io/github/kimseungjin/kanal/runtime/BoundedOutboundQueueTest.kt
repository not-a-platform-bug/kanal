package io.github.kimseungjin.kanal.runtime

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BoundedOutboundQueueTest {
    @Test
    fun `enqueues until capacity is reached`() {
        val queue = BoundedOutboundQueue<String>(capacity = 2, policy = BackpressurePolicy.SUSPEND)

        assertEquals(OutboundQueueOfferResult.ENQUEUED, queue.offer("a"))
        assertEquals(OutboundQueueOfferResult.ENQUEUED, queue.offer("b"))
        assertEquals(2, queue.size())
        assertEquals(listOf("a", "b"), queue.snapshot())
    }

    @Test
    fun `drop oldest removes the oldest item and enqueues the latest item`() {
        val metrics = RuntimeMetrics()
        val queue = BoundedOutboundQueue<String>(capacity = 2, policy = BackpressurePolicy.DROP_OLDEST, metrics = metrics)

        queue.offer("a")
        queue.offer("b")

        assertEquals(OutboundQueueOfferResult.DROPPED_OLDEST, queue.offer("c"))
        assertEquals(listOf("b", "c"), queue.snapshot())
        assertEquals(1, metrics.snapshot().droppedOutboundMessages)
        assertEquals(1, metrics.snapshot().slowConsumerSignals)
        assertEquals(1, metrics.snapshot().dropsByPolicy.getValue(BackpressurePolicy.DROP_OLDEST))
        assertEquals(1, metrics.snapshot().slowConsumerSignalsByPolicy.getValue(BackpressurePolicy.DROP_OLDEST))
    }

    @Test
    fun `drop latest keeps existing items when full`() {
        val queue = BoundedOutboundQueue<String>(capacity = 2, policy = BackpressurePolicy.DROP_LATEST)

        queue.offer("a")
        queue.offer("b")

        assertEquals(OutboundQueueOfferResult.DROPPED_LATEST, queue.offer("c"))
        assertEquals(listOf("a", "b"), queue.snapshot())
    }

    @Test
    fun `disconnect policy reports disconnect without mutating queue`() {
        val metrics = RuntimeMetrics()
        val queue = BoundedOutboundQueue<String>(capacity = 1, policy = BackpressurePolicy.DISCONNECT, metrics = metrics)

        queue.offer("a")

        assertEquals(OutboundQueueOfferResult.DISCONNECT, queue.offer("b"))
        assertEquals(listOf("a"), queue.snapshot())
        assertEquals(1, metrics.snapshot().disconnectsByPolicy.getValue(BackpressurePolicy.DISCONNECT))
        assertEquals(1, metrics.snapshot().slowConsumerSignalsByPolicy.getValue(BackpressurePolicy.DISCONNECT))
    }

    @Test
    fun `suspend policy reports would suspend when full`() {
        val queue = BoundedOutboundQueue<String>(capacity = 1, policy = BackpressurePolicy.SUSPEND)

        queue.offer("a")

        assertEquals(OutboundQueueOfferResult.WOULD_SUSPEND, queue.offer("b"))
        assertEquals("a", queue.poll())
        assertNull(queue.poll())
    }
}
