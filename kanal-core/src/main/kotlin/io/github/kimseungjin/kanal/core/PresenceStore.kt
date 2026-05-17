package io.github.kimseungjin.kanal.core

interface PresenceStore {
    fun track(channel: ChannelAddress, entry: PresenceEntry)

    fun untrack(channel: ChannelAddress, key: String)

    fun list(channel: ChannelAddress): List<PresenceEntry>
}
