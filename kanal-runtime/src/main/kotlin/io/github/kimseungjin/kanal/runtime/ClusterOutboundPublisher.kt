package io.github.kimseungjin.kanal.runtime

import io.github.kimseungjin.kanal.core.ChannelAddress

fun interface ClusterOutboundPublisher {
    fun broadcast(
        address: ChannelAddress,
        channel: String,
        payload: Any,
    )

    companion object {
        val NoOp: ClusterOutboundPublisher = ClusterOutboundPublisher { _, _, _ -> }
    }
}
