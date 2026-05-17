package io.github.kimseungjin.kanal.core

fun interface OutboundPublisher {
    fun broadcast(address: ChannelAddress, message: Any)

    fun sendToSession(session: SessionDescriptor, message: Any) {
        broadcast(ChannelAddress(ChannelPattern("_self:${session.id}")), message)
    }

    companion object {
        val NoOp: OutboundPublisher =
            OutboundPublisher { _, _ ->
            }
    }
}
