package io.github.kimseungjin.kanal.runtime

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

interface RuntimeHandlerExecutor : AutoCloseable {
    fun execute(task: () -> Unit)

    override fun close() {
    }
}

object DirectRuntimeHandlerExecutor : RuntimeHandlerExecutor {
    override fun execute(task: () -> Unit) {
        task()
    }
}

class ExecutorRuntimeHandlerExecutor(
    private val executor: ExecutorService,
) : RuntimeHandlerExecutor {
    override fun execute(task: () -> Unit) {
        executor.execute(task)
    }

    override fun close() {
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }
}

enum class RuntimeHandlerExecution {
    DIRECT,
    VIRTUAL_THREADS,
}

fun RuntimeHandlerExecution.createExecutor(threadNamePrefix: String): RuntimeHandlerExecutor =
    when (this) {
        RuntimeHandlerExecution.DIRECT -> DirectRuntimeHandlerExecutor
        RuntimeHandlerExecution.VIRTUAL_THREADS ->
            ExecutorRuntimeHandlerExecutor(
                Executors.newThreadPerTaskExecutor(virtualThreadFactory(threadNamePrefix)),
            )
    }

private fun virtualThreadFactory(threadNamePrefix: String): ThreadFactory {
    val sequence = AtomicLong()

    return Thread.ofVirtual()
        .name("$threadNamePrefix-", sequence.get())
        .factory()
}
