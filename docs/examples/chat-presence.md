# Chat Presence Example

[한국어](chat-presence.ko.md) | English

This example shows the product shape Kanal is aiming for: define realtime behavior as application code, keep presence built in, and make backpressure choices explicit.

The sample lives in:

```text
kanal-samples/chat-presence
```

It is intentionally honest about the current project state. Kanal does not have the WebSocket runtime module yet, so this sample does not open a working `/realtime` endpoint today. What it does provide is a compiling Spring Boot application that defines a `RealtimeApplication` bean with realistic chat and typing channels. When `kanal-runtime` arrives, this is the kind of application code the runtime should execute.

## What The Example Models

The sample defines two channels:

- `chat/{roomId}`
  Room chat messages, join/leave lifecycle, presence tracking, and presence change broadcasts.
- `chat/{roomId}/typing`
  Ephemeral typing indicators with a different backpressure policy.

The interesting product idea is that chat messages and typing indicators have different delivery semantics:

- chat messages use `DROP_OLDEST`
  If a client is slow, older queued messages are less useful than keeping the client closer to the latest room state.
- typing indicators use `DROP_LATEST`
  A typing signal is ephemeral. If the queue is already under pressure, adding more typing updates is usually not worth it.

These policies are not perfect for every app. The point is that Kanal makes the decision visible at the channel boundary.

## Files

```text
kanal-samples/chat-presence/
  build.gradle.kts
  src/main/kotlin/io/github/kimseungjin/kanal/samples/chatpresence/
    ChatPresenceApplication.kt
    ChatPresenceChannels.kt
  src/test/kotlin/io/github/kimseungjin/kanal/samples/chatpresence/
    ChatPresenceApplicationTest.kt
```

## Application Entry Point

The Spring Boot app is deliberately small:

```kotlin
@SpringBootApplication
class ChatPresenceApplication

fun main(args: Array<String>) {
    runApplication<ChatPresenceApplication>(*args)
}
```

The important part is not the boot class. The important part is the `RealtimeApplication` bean.

## Channel Definition

The chat channel is defined with the core DSL:

```kotlin
import io.github.kimseungjin.kanal.core.dsl.channel
import io.github.kimseungjin.kanal.core.dsl.realtime

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

                send(ChatSystemNotice(roomId = roomId, message = "Welcome to $roomId."))

                broadcast(
                    RoomPresenceChanged(
                        roomId = roomId,
                        action = PresenceAction.JOINED,
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
    }
```

This shows the intended Kanal style:

- path parameters are available through `address.parameters`
- authenticated identity is available through `session.userId`
- application metadata is available through `session.attributes`
- presence is a first-class API on the handler context
- outbound behavior is expressed as `send` or `broadcast`

## Message Types

The sample keeps message payloads as ordinary Kotlin data classes:

```kotlin
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
```

This is important for Kanal's direction. Application teams should think in domain messages, not raw frames.

## Expected Runtime Flow

When the runtime exists, the expected flow for joining a room should be:

1. client connects to the configured endpoint
2. client sends a `join` frame for `chat/general`
3. runtime resolves `chat/general` against `chat/{roomId}`
4. runtime extracts `roomId=general`
5. runtime creates `ChannelContext`
6. `onJoin` tracks presence for the session
7. runtime sends a welcome notice to the joining session
8. runtime broadcasts the updated room presence to the room

Candidate frame:

```json
{
  "ref": "1",
  "event": "join",
  "channel": "chat/general",
  "payload": {}
}
```

Candidate outbound presence event:

```json
{
  "event": "message",
  "channel": "chat/general",
  "payload": {
    "roomId": "general",
    "action": "JOINED",
    "changedKey": "user-123",
    "members": [
      {
        "key": "user-123",
        "metadata": {
          "roomId": "general",
          "displayName": "Mina",
          "device": "web"
        }
      }
    ]
  }
}
```

## Why This Example Matters

This example is small, but it exercises the concepts that should make Kanal different:

- channel as the unit of behavior
- typed messages
- path parameters
- session identity
- presence as a built-in concept
- lifecycle hooks
- channel-specific backpressure
- Spring Boot integration

It is also a useful acceptance target for the future runtime. Once `kanal-runtime` is implemented, this sample should become the first end-to-end demo.

## Run The Sample Tests

```bash
./gradlew :kanal-samples:chat-presence:test
```

The test verifies that Spring Boot loads the user-defined `RealtimeApplication` and that both channels are registered with the expected backpressure policies.
