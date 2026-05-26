package io.github.kimseungjin.kanal.runtime

fun interface RuntimeTransportSession {
    fun send(frame: RealtimeFrame)

    fun close(reason: String) {
    }
}
