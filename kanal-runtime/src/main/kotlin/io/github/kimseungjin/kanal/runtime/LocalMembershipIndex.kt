package io.github.kimseungjin.kanal.runtime

import io.github.kimseungjin.kanal.core.ChannelAddress
import io.github.kimseungjin.kanal.core.SessionDescriptor
import java.util.concurrent.ConcurrentHashMap

class LocalMembershipIndex(
    private val metrics: RuntimeMetrics? = null,
) {
    private val sessionsByChannel = ConcurrentHashMap<ChannelAddress, MutableSet<String>>()
    private val channelsBySession = ConcurrentHashMap<String, MutableSet<ChannelAddress>>()

    fun join(
        session: SessionDescriptor,
        address: ChannelAddress,
    ): Boolean {
        val addedToChannel =
            sessionsByChannel
                .computeIfAbsent(address) { ConcurrentHashMap.newKeySet() }
                .add(session.id)

        val addedToSession =
            channelsBySession
                .computeIfAbsent(session.id) { ConcurrentHashMap.newKeySet() }
                .add(address)

        val added = addedToChannel || addedToSession
        if (added) {
            metrics?.membershipJoined()
        }

        return added
    }

    fun leave(
        sessionId: String,
        address: ChannelAddress,
    ): Boolean {
        val removedFromChannel = sessionsByChannel[address]?.remove(sessionId) == true
        if (sessionsByChannel[address]?.isEmpty() == true) {
            sessionsByChannel.remove(address)
        }

        val removedFromSession = channelsBySession[sessionId]?.remove(address) == true
        if (channelsBySession[sessionId]?.isEmpty() == true) {
            channelsBySession.remove(sessionId)
        }

        val removed = removedFromChannel || removedFromSession
        if (removed) {
            metrics?.membershipLeft()
        }

        return removed
    }

    fun removeSession(sessionId: String): Set<ChannelAddress> {
        val channels = channelsBySession.remove(sessionId)?.toSet().orEmpty()

        channels.forEach { address ->
            sessionsByChannel[address]?.remove(sessionId)
            if (sessionsByChannel[address]?.isEmpty() == true) {
                sessionsByChannel.remove(address)
            }
        }

        repeat(channels.size) {
            metrics?.membershipLeft()
        }

        return channels
    }

    fun sessions(address: ChannelAddress): Set<String> =
        sessionsByChannel[address]?.toSet().orEmpty()

    fun contains(
        sessionId: String,
        address: ChannelAddress,
    ): Boolean = sessionsByChannel[address]?.contains(sessionId) == true

    fun broadcastTargets(address: ChannelAddress): Set<String> {
        val targets = sessions(address)
        metrics?.recordBroadcastFanOut(targets.size)
        return targets
    }

    fun forEachBroadcastTarget(
        address: ChannelAddress,
        block: (String) -> Unit,
    ) {
        val targets = sessionsByChannel[address].orEmpty()
        metrics?.recordBroadcastFanOut(targets.size)
        targets.forEach(block)
    }

    fun channels(sessionId: String): Set<ChannelAddress> =
        channelsBySession[sessionId]?.toSet().orEmpty()

    fun activeMemberships(): Int =
        sessionsByChannel.values.sumOf { it.size }
}
