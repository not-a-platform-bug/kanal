# Chat Presence 예제

한국어 | [English](chat-presence.md)

이 샘플은 Kanal local WebSocket runtime으로 동작하는 runnable Spring Boot chat app입니다.

정의하는 channel:

- `chat/{roomId}`: room message, join/leave hook, presence change
- `chat/{roomId}/typing`: ephemeral typing indicator

Chat message는 `DROP_OLDEST`, typing signal은 `DROP_LATEST`를 사용합니다. 핵심은 delivery tradeoff를 channel 경계에서 명시적으로 보여주는 것입니다.

## 실행

```bash
./gradlew :kanal-samples:chat-presence:bootRun
```

이후 브라우저에서 엽니다.

```text
http://localhost:8080
```

정적 browser client는 다음 endpoint에 연결합니다.

```text
ws://localhost:8080/realtime
```

## Frames

Room join:

```json
{
  "ref": "1",
  "event": "join",
  "channel": "chat/general",
  "payload": {}
}
```

Chat message 전송:

```json
{
  "ref": "2",
  "event": "message",
  "channel": "chat/general",
  "payload": {
    "messageId": "m1",
    "body": "hello"
  }
}
```

Typing signal 전송:

```json
{
  "ref": "3",
  "event": "message",
  "channel": "chat/general/typing",
  "payload": {
    "typing": true
  }
}
```

## 테스트

```bash
./gradlew :kanal-samples:chat-presence:test
```

테스트는 app을 random port로 띄우고, 실제 WebSocket client로 `chat/general`에 join한 뒤 message를 보내 reply/message frame을 검증합니다.
