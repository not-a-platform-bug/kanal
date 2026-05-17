package io.github.kimseungjin.kanal.core

enum class BackpressurePolicy {
    SUSPEND,
    DROP_OLDEST,
    DROP_LATEST,
    DISCONNECT,
}
