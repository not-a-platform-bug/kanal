package io.github.kimseungjin.kanal.benchmarks

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import io.github.kimseungjin.kanal.core.InMemoryPresenceStore
import io.github.kimseungjin.kanal.core.SessionDescriptor
import io.github.kimseungjin.kanal.core.dsl.channel
import io.github.kimseungjin.kanal.core.dsl.realtime
import io.github.kimseungjin.kanal.runtime.BoundedOutboundQueue
import io.github.kimseungjin.kanal.runtime.LocalRealtimeRuntime
import io.github.kimseungjin.kanal.runtime.RealtimeFrame
import io.github.kimseungjin.kanal.runtime.RealtimeFrameEvents
import io.github.kimseungjin.kanal.runtime.RealtimeRuntimeOptions
import io.github.kimseungjin.kanal.runtime.RuntimeTransportSession
import java.time.Duration
import kotlin.system.measureNanoTime

data class BenchmarkMessage(
    val body: String,
)

fun main(args: Array<String>) {
    val iterations = args.option("--iterations")?.toInt() ?: 100_000
    val channels = args.option("--channels")?.toInt() ?: 1_000
    val sessions = args.option("--sessions")?.toInt() ?: 1_000

    println("Kanal benchmark fixture")
    println("iterations=$iterations channels=$channels sessions=$sessions")
    println()

    println(runResolutionBenchmark(channels = channels, iterations = iterations).format())
    println(runBroadcastBenchmark(sessions = sessions).format())
    println(runQueueBenchmark(iterations = iterations).format())
}

fun runResolutionBenchmark(
    channels: Int,
    iterations: Int,
): BenchmarkResult {
    val app =
        realtime {
            repeat(channels) { index ->
                channel<BenchmarkMessage>("rooms/$index/{roomId}") {
                }
            }
        }

    repeat(10_000.coerceAtMost(iterations)) { index ->
        app.resolve("rooms/${index % channels}/general")
    }

    val elapsed =
        measureNanoTime {
            repeat(iterations) { index ->
                checkNotNull(app.resolve("rooms/${index % channels}/general"))
            }
        }

    return BenchmarkResult(
        name = "channel-resolution",
        operations = iterations,
        elapsedNanos = elapsed,
    )
}

fun runBroadcastBenchmark(sessions: Int): BenchmarkResult {
    val app =
        realtime {
            channel<BenchmarkMessage>("chat/{roomId}") {
                onMessage { message -> broadcast(message) }
            }
        }
    val runtime =
        LocalRealtimeRuntime(
            application = app,
            presenceStore = InMemoryPresenceStore(),
            options =
                RealtimeRuntimeOptions(
                    heartbeatInterval = Duration.ZERO,
                    outboundQueueCapacity = 1024,
                ),
        )

    repeat(sessions) { index ->
        runtime.connect(SessionDescriptor(id = "s$index"), CountingTransportSession)
        runtime.receive("s$index", RealtimeFrame(event = RealtimeFrameEvents.JOIN, channel = "chat/general"))
    }

    val elapsed =
        measureNanoTime {
            runtime.receive(
                "s0",
                RealtimeFrame(
                    event = RealtimeFrameEvents.MESSAGE,
                    channel = "chat/general",
                    payload = BenchmarkMessage("hello"),
                ),
            )
        }

    runtime.close()

    return BenchmarkResult(
        name = "local-broadcast-fanout",
        operations = sessions,
        elapsedNanos = elapsed,
    )
}

fun runQueueBenchmark(iterations: Int): BenchmarkResult {
    val queue =
        BoundedOutboundQueue<Int>(
            capacity = 1024,
            policy = BackpressurePolicy.DROP_OLDEST,
        )

    val elapsed =
        measureNanoTime {
            repeat(iterations) { index ->
                queue.offer(index)
            }
        }

    return BenchmarkResult(
        name = "bounded-queue-offer",
        operations = iterations,
        elapsedNanos = elapsed,
    )
}

data class BenchmarkResult(
    val name: String,
    val operations: Int,
    val elapsedNanos: Long,
) {
    val averageNanos: Long = if (operations == 0) 0 else elapsedNanos / operations
    val operationsPerSecond: Long =
        if (elapsedNanos == 0L) 0L else (operations * 1_000_000_000L) / elapsedNanos

    fun format(): String =
        "$name: operations=$operations elapsedMs=${elapsedNanos / 1_000_000} avgNanos=$averageNanos opsPerSec=$operationsPerSecond"
}

private object CountingTransportSession : RuntimeTransportSession {
    override fun send(frame: RealtimeFrame) {
    }
}

private fun Array<String>.option(name: String): String? {
    val index = indexOf(name)
    if (index < 0 || index == lastIndex) {
        return null
    }

    return this[index + 1]
}
