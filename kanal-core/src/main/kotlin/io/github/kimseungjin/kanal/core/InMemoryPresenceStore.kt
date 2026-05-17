package io.github.kimseungjin.kanal.core

import java.util.concurrent.ConcurrentHashMap

class InMemoryPresenceStore : PresenceStore {
    private val entries = ConcurrentHashMap<ChannelAddress, ConcurrentHashMap<String, PresenceEntry>>()

    override fun track(channel: ChannelAddress, entry: PresenceEntry) {
        entries.computeIfAbsent(channel) { ConcurrentHashMap() }[entry.key] = entry
    }

    override fun untrack(channel: ChannelAddress, key: String) {
        entries[channel]?.remove(key)
        if (entries[channel]?.isEmpty() == true) {
            entries.remove(channel)
        }
    }

    override fun list(channel: ChannelAddress): List<PresenceEntry> =
        entries[channel]
            ?.values
            ?.sortedBy { it.joinedAt }
            .orEmpty()
}
