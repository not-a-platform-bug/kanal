package io.github.kimseungjin.kanal.core

class PresenceContext(
    private val channel: ChannelAddress,
    private val store: PresenceStore,
) {
    fun track(
        key: String,
        metadata: Map<String, String> = emptyMap(),
    ) {
        store.track(channel, PresenceEntry(key = key, metadata = metadata))
    }

    fun untrack(key: String) {
        store.untrack(channel, key)
    }

    fun list(): List<PresenceEntry> = store.list(channel)
}
