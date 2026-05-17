package io.github.kimseungjin.kanal.samples.chatpresence

import io.github.kimseungjin.kanal.core.BackpressurePolicy
import io.github.kimseungjin.kanal.core.PresenceEntry
import io.github.kimseungjin.kanal.core.RealtimeApplication
import io.github.kimseungjin.kanal.core.dsl.channel
import io.github.kimseungjin.kanal.core.dsl.realtime
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant

@Configuration
class ChatPresenceChannels {
    @Bean
    fun chatPresenceRealtimeApplication(): RealtimeApplication =
        realtime {
            channel<ChatMessage>("chat/{roomId}") {
                description("Room chat messages with presence-aware join and leave hooks")
                backpressure(BackpressurePolicy.DROP_OLDEST)

                onJoin {
                    val roomId = address.parameters.getValue("roomId")
                    val memberKey = session.userId ?: session.id

                    presence.track(
                        key = memberKey,
                        metadata = session.presenceMetadata(roomId),
                    )

                    send(
                        ChatSystemNotice(
                            roomId = roomId,
                            message = "Welcome to $roomId.",
                        ),
                    )

                    broadcast(
                        RoomPresenceChanged(
                            roomId = roomId,
                            action = PresenceAction.JOINED,
                            changedKey = memberKey,
                            members = presence.list().toMemberPresence(),
                        ),
                    )
                }

                onLeave {
                    val roomId = address.parameters.getValue("roomId")
                    val memberKey = session.userId ?: session.id

                    presence.untrack(memberKey)

                    broadcast(
                        RoomPresenceChanged(
                            roomId = roomId,
                            action = PresenceAction.LEFT,
                            changedKey = memberKey,
                            members = presence.list().toMemberPresence(),
                        ),
                    )
                }

                onMessage { message ->
                    val roomId = address.parameters.getValue("roomId")

                    broadcast(
                        message.copy(
                            roomId = roomId,
                            authorId = message.authorId.ifBlank { session.userId ?: session.id },
                        ),
                    )
                }
            }

            channel<TypingSignal>("chat/{roomId}/typing") {
                description("Ephemeral typing indicators for a chat room")
                backpressure(BackpressurePolicy.DROP_LATEST)

                onMessage { signal ->
                    val roomId = address.parameters.getValue("roomId")

                    broadcast(
                        signal.copy(
                            roomId = roomId,
                            userId = signal.userId.ifBlank { session.userId ?: session.id },
                        ),
                    )
                }
            }
        }
}

data class ChatMessage(
    val messageId: String,
    val roomId: String = "",
    val authorId: String = "",
    val body: String,
    val sentAt: Instant = Instant.now(),
)

data class TypingSignal(
    val roomId: String = "",
    val userId: String = "",
    val typing: Boolean,
    val occurredAt: Instant = Instant.now(),
)

data class ChatSystemNotice(
    val roomId: String,
    val message: String,
    val occurredAt: Instant = Instant.now(),
)

data class RoomPresenceChanged(
    val roomId: String,
    val action: PresenceAction,
    val changedKey: String,
    val members: List<MemberPresence>,
    val occurredAt: Instant = Instant.now(),
)

data class MemberPresence(
    val key: String,
    val metadata: Map<String, String>,
    val joinedAt: Instant,
)

enum class PresenceAction {
    JOINED,
    LEFT,
}

private fun io.github.kimseungjin.kanal.core.SessionDescriptor.presenceMetadata(roomId: String): Map<String, String> =
    buildMap {
        put("roomId", roomId)
        attributes["displayName"]?.takeIf { it.isNotBlank() }?.let { put("displayName", it) }
        attributes["device"]?.takeIf { it.isNotBlank() }?.let { put("device", it) }
    }

private fun List<PresenceEntry>.toMemberPresence(): List<MemberPresence> =
    map { entry ->
        MemberPresence(
            key = entry.key,
            metadata = entry.metadata,
            joinedAt = entry.joinedAt,
        )
    }
