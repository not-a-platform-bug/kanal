# Chat Presence Example

[한국어](chat-presence.ko.md) | English

This sample is a runnable Spring Boot chat app backed by Kanal's local WebSocket runtime.

It defines:

- `chat/{roomId}` for room messages, join/leave hooks, and presence changes
- `chat/{roomId}/typing` for ephemeral typing indicators

Chat messages use `DROP_OLDEST`; typing signals use `DROP_LATEST`. The point is to make delivery tradeoffs visible at the channel boundary.

## Run

```bash
./gradlew :kanal-samples:chat-presence:bootRun
```

Then open:

```text
http://localhost:8080
```

The static browser client connects to:

```text
ws://localhost:8080/realtime
```

## Frames

Join a room:

```json
{
  "ref": "1",
  "event": "join",
  "channel": "chat/general",
  "payload": {}
}
```

Send a chat message:

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

Send a typing signal:

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

## Test

```bash
./gradlew :kanal-samples:chat-presence:test
```

The test boots the app on a random port, connects through a real WebSocket client, joins `chat/general`, sends a message, and verifies reply/message frames.
