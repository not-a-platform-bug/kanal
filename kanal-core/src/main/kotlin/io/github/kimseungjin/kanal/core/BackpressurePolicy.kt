package io.github.kimseungjin.kanal.core

enum class BackpressurePolicy {
    /**
     * Signals that delivery would need to wait when the outbound queue is full.
     *
     * Kanal's current runtime does not block transport threads; it reports the
     * pressure as a non-blocking rejection signal for metrics and diagnostics.
     */
    SUSPEND,
    DROP_OLDEST,
    DROP_LATEST,
    DISCONNECT,
}
