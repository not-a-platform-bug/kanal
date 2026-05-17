# Chat Presence 예제

한국어 | [English](chat-presence.md)

이 예제는 Kanal이 지향하는 제품 형태를 보여줍니다. Realtime behavior를 애플리케이션 코드로 정의하고, presence를 기본 기능으로 다루며, backpressure 결정을 channel 경계에서 명시적으로 표현합니다.

샘플 위치:

```text
kanal-samples/chat-presence
```

이 예제는 현재 프로젝트 상태를 솔직하게 반영합니다. Kanal에는 아직 WebSocket runtime module이 없기 때문에 지금 당장 동작하는 `/realtime` endpoint를 열지는 않습니다. 대신 현실적인 chat channel과 typing channel을 정의하는 컴파일 가능한 Spring Boot application을 제공합니다. 이후 `kanal-runtime`이 생기면 runtime이 실행해야 할 애플리케이션 코드의 기준점이 됩니다.

## 예제가 모델링하는 것

샘플은 두 channel을 정의합니다.

- `chat/{roomId}`
  Room chat message, join/leave lifecycle, presence tracking, presence change broadcast.
- `chat/{roomId}/typing`
  다른 backpressure policy를 가진 ephemeral typing indicator.

흥미로운 제품적 아이디어는 chat message와 typing indicator의 delivery semantics가 다르다는 점입니다.

- chat message는 `DROP_OLDEST`
  client가 느릴 때 오래된 queued message보다 최신 room state에 가깝게 유지하는 것이 더 유용할 수 있습니다.
- typing indicator는 `DROP_LATEST`
  typing signal은 ephemeral합니다. queue가 이미 압박을 받고 있다면 typing update를 더 넣는 것이 대체로 가치가 낮습니다.

이 policy가 모든 앱에 완벽하다는 뜻은 아닙니다. 중요한 점은 Kanal이 이 결정을 channel 경계에서 보이게 만든다는 것입니다.

## 파일

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

Spring Boot app은 의도적으로 작게 유지합니다.

```kotlin
@SpringBootApplication
class ChatPresenceApplication

fun main(args: Array<String>) {
    runApplication<ChatPresenceApplication>(*args)
}
```

중요한 부분은 boot class가 아니라 `RealtimeApplication` bean입니다.

## Channel Definition

Chat channel은 core DSL로 정의합니다.

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

이 코드는 Kanal이 지향하는 스타일을 보여줍니다.

- path parameter는 `address.parameters`에서 접근
- authenticated identity는 `session.userId`에서 접근
- application metadata는 `session.attributes`에서 접근
- presence는 handler context의 first-class API
- outbound behavior는 `send` 또는 `broadcast`로 표현

## Message Types

샘플은 message payload를 평범한 Kotlin data class로 둡니다.

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

이건 Kanal의 방향에서 중요합니다. 애플리케이션 팀은 raw frame이 아니라 domain message로 생각할 수 있어야 합니다.

## 예상 Runtime Flow

Runtime이 생기면 room join은 다음 흐름이 되어야 합니다.

1. client가 configured endpoint에 연결
2. client가 `chat/general`에 대한 `join` frame 전송
3. runtime이 `chat/general`을 `chat/{roomId}`와 resolve
4. runtime이 `roomId=general` 추출
5. runtime이 `ChannelContext` 생성
6. `onJoin`이 session presence track
7. runtime이 joining session에 welcome notice 전송
8. runtime이 room에 updated room presence broadcast

후보 frame:

```json
{
  "ref": "1",
  "event": "join",
  "channel": "chat/general",
  "payload": {}
}
```

후보 outbound presence event:

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

## 왜 이 예제가 중요한가

작은 예제지만 Kanal을 다르게 만드는 핵심 개념을 모두 건드립니다.

- behavior의 단위로서 channel
- typed messages
- path parameters
- session identity
- built-in presence
- lifecycle hooks
- channel-specific backpressure
- Spring Boot integration

또한 future runtime의 acceptance target이 됩니다. `kanal-runtime`이 구현되면 이 샘플이 첫 end-to-end demo가 되어야 합니다.

## 샘플 테스트 실행

```bash
./gradlew :kanal-samples:chat-presence:test
```

테스트는 Spring Boot가 user-defined `RealtimeApplication`을 load하는지, 두 channel이 예상한 backpressure policy와 함께 등록되는지 확인합니다.
