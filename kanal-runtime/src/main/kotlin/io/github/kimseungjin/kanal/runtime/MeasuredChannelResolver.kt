package io.github.kimseungjin.kanal.runtime

import io.github.kimseungjin.kanal.core.ChannelResolution
import io.github.kimseungjin.kanal.core.RealtimeApplication

class MeasuredChannelResolver(
    private val application: RealtimeApplication,
    private val metrics: RuntimeMetrics,
) {
    fun resolve(path: String): ChannelResolution? {
        val startedAt = System.nanoTime()

        return try {
            application.resolve(path)
        } finally {
            metrics.recordChannelResolution(System.nanoTime() - startedAt)
        }
    }
}
