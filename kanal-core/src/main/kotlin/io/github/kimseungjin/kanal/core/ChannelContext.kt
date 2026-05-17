package io.github.kimseungjin.kanal.core

class ChannelContext(
    val session: SessionDescriptor,
    val address: ChannelAddress,
    private val publisher: OutboundPublisher,
    store: PresenceStore,
) {
    val presence: PresenceContext = PresenceContext(address, store)

    fun send(message: Any) {
        publisher.sendToSession(session, message)
    }

    fun broadcast(message: Any) {
        publisher.broadcast(address, message)
    }
}
