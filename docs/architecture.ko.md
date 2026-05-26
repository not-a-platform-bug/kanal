# 아키텍처

한국어 | [English](architecture.md)

Kanal은 애플리케이션 모델 중심 realtime framework입니다. 아키텍처는 realtime 기능을 만들 때 제품 팀이 실제로 다루는 개념인 channel, session, presence, membership, dispatch, outbound delivery, diagnostics를 중심으로 구성됩니다.

## 계층

Kanal은 core model을 안정적으로 유지하고 runtime과 framework integration을 독립적으로 발전시킬 수 있도록 계층을 분리해야 합니다.

1. `kanal-core`
   Channel definition, channel pattern, session, presence, context, message handler, backpressure policy 등 시스템의 언어를 정의합니다.
2. `kanal-runtime`
   Membership index, bounded outbound queue, local metrics, local connection lifecycle, frame decoding, handler dispatch, heartbeat, runtime shutdown 같은 runtime foundation을 담당합니다.
3. `kanal-spring-boot-starter`
   Autoconfiguration, properties, security integration, metrics, actuator endpoint를 통해 runtime을 Spring Boot에 연결합니다.
4. `cluster adapters`
   Node presence, user routing, channel membership, presence state 같은 metadata를 복제합니다. socket은 연결을 수락한 node가 계속 소유합니다.

핵심 규칙은 framework integration이 model에 의존해야지, model을 정의하면 안 된다는 것입니다.

## 핵심 개념

### Channel

Channel은 realtime behavior의 기본 단위입니다.

예시:

- `chat/{roomId}`
- `notifications/{userId}`
- `presence/{workspaceId}`

Channel이 소유하는 것:

- path pattern
- message type
- join, leave, message handler
- backpressure policy
- description 같은 product-facing metadata

첫 runtime에는 `chat/alpha` 같은 실제 path를 channel definition과 `roomId=alpha` 같은 parameter로 바꾸는 resolver가 필요합니다.

현재 core는 이미 registration 시 channel pattern을 compile하고, `RealtimeApplication.resolve(path)`를 통해 resolver를 노출합니다.

### Session

Session은 애플리케이션 관점에서 하나의 살아 있는 client connection을 나타냅니다.

포함해야 할 정보:

- 생성된 session id
- optional authenticated user id
- principal attributes
- local attributes
- diagnostics에 유용한 connection metadata

한 user는 여러 session을 가질 수 있습니다. Kanal은 user 하나가 connection 하나라는 가정을 피하고, 이 관계를 직접 모델링해야 합니다.

### Membership

Membership은 session과 channel address 사이의 관계입니다. Presence와는 별개의 개념입니다.

Membership이 답하는 질문:

- 어떤 session들이 이 channel address에 subscribe되어 있는가?
- 이 session은 어떤 channel address들에 join했는가?
- broadcast는 어디로 전달되어야 하는가?
- connection이 닫힐 때 무엇을 정리해야 하는가?

Presence는 사용자에게 보이는 상태입니다. Membership은 runtime routing 상태입니다.

### Presence

Presence는 channel 안에서 현재 보이는 사람이나 대상을 표현하고, 가벼운 metadata를 붙입니다.

최소 contract:

- `track`
- `untrack`
- `list`

In-memory 구현은 local runtime에 적합합니다. 이후 cluster adapter는 socket ownership을 옮기지 않고 presence metadata를 복제할 수 있습니다.

### Context

Handler는 `ChannelContext`를 받습니다. 이 context는 애플리케이션 개발자가 사용하는 안정적인 표면이 되어야 합니다.

- session data
- resolved channel address
- 추출된 path parameters
- presence access
- outbound publishing operations
- tracing과 security metadata
- 필요할 경우 cancellation과 timeout signal

Context는 Kotlin에서 기분 좋게 사용할 수 있어야 합니다. Runtime concern은 필요할 때 접근 가능해야 하지만 handler API를 지배하면 안 됩니다.

## Local Runtime 방향

첫 runtime은 선명한 single-node experience에 집중해야 합니다.

책임:

- WebSocket connection 수락
- 작은 Kanal wire protocol decode
- channel pattern 해석
- join, leave, message event dispatch
- local membership index 유지
- session lifecycle 추적
- bounded outbound queue 적용
- channel backpressure policy 적용
- heartbeat frame 전송
- graceful shutdown

Runtime은 transport I/O에는 event loop를 사용하고, user handler는 blocking-style model로 실행할 수 있습니다. 이렇게 하면 public API가 일반 애플리케이션 코드에 친숙하고, virtual thread 실행 모델로 확장할 여지도 남습니다.

## Wire Protocol 형태

초기 protocol은 지루할 정도로 단순하고 관찰 가능해야 합니다.

현재 frame:

```json
{
  "ref": "1",
  "event": "message",
  "channel": "chat/alpha",
  "payload": {
    "body": "hello"
  }
}
```

초기 event:

- `join`
- `leave`
- `message`
- `heartbeat`
- `error`
- `reply`

Reply payload는 다음 envelope으로 감쌉니다.

```json
{
  "event": "reply",
  "payload": {
    "event": "join",
    "status": "ok",
    "response": {}
  }
}
```

Error payload는 안정적인 code를 사용합니다.

```json
{
  "event": "error",
  "payload": {
    "code": "malformed_frame",
    "message": "Malformed frame"
  }
}
```

첫 runtime에서 durable delivery를 약속하면 안 됩니다. 시스템이 실제로 제공하지 않는 보장을 암시하는 것보다, 명확한 at-most-once local delivery model이 훨씬 낫습니다.

현재 runtime foundation은 이 frame 형태를 기준으로 `join`, `leave`, `message`, `heartbeat` dispatch를 지원합니다. 또한 scheduled heartbeat frame을 보내고, heartbeat-timeout session을 닫으며, runtime close 시 membership을 정리합니다. Spring Boot starter는 `/realtime` 기본 endpoint에서 text WebSocket message를 JSON frame으로 decode해 runtime으로 전달합니다.

## Backpressure

Realtime system은 edge에서 먼저 실패하므로 backpressure는 명시적이어야 합니다.

기존 policy:

- `SUSPEND`
- `DROP_OLDEST`
- `DROP_LATEST`
- `DISCONNECT`

Runtime은 queue depth, dropped messages, policy로 인한 disconnect, slow consumer에 대한 metrics를 노출해야 합니다.

## 성능 모델

Kanal의 성능 모델은 단일 aggregate throughput 숫자보다 realtime hot path에 집중해야 합니다.

Local hot path:

1. frame 수신
2. envelope decode
3. channel address resolve
4. typed payload decode
5. handler invoke
6. outbound message publish
7. membership index에서 target session 조회
8. outbound frame enqueue
9. socket flush

중요한 설계 결과:

- `ChannelPattern`은 registration 시 compile and validate되어야 합니다.
- channel resolution은 매 frame마다 모든 registered pattern을 scan하면 안 됩니다.
- runtime membership은 `ChannelAddress -> SessionId set`과 `SessionId -> ChannelAddress set`을 모두 유지해야 합니다.
- outbound queue는 기본적으로 bounded해야 합니다.
- fan-out size, queue depth, handler latency, dropped messages는 별도로 측정되어야 합니다.

초기 코드 foundation에는 local JSON frame dispatch path와 Spring WebSocket adapter까지 포함되어 있습니다.

`kanal-benchmarks` module은 channel resolution, local broadcast fan-out, bounded queue offer behavior를 검증하는 lightweight executable fixture를 제공합니다. 현재 실행 명령과 테스트된 runtime path는 [README](../README.ko.md)에 정리되어 있습니다.

## Cluster 방향

Cluster support는 connection migration을 의미하면 안 됩니다.

복제할 수 있는 것:

- node registry
- user-to-node routing metadata
- channel membership metadata
- presence metadata
- cross-node broadcast envelope

Local에 남아야 하는 것:

- TCP와 WebSocket connection
- outbound queue
- connection heartbeat state
- local session cleanup

이 방향은 현실적인 distributed model을 줍니다. Connection을 소유한 node로 route하고, route에 필요한 metadata만 복제하며, failure behavior를 이해 가능한 상태로 유지합니다.

`kanal-cluster-redis` module은 metadata data class, 안정적인 Redis keyspace helper, TTL/options validation, `RedisClusterMetadataStore` contract로 이 방향의 시작점을 제공합니다. 아직 동작하는 Redis 구현을 제공한다고 말하지는 않습니다.

## Observability

Kanal은 diagnostics를 제품의 일부로 다뤄야 합니다.

초기 metrics:

- active sessions
- active channel memberships
- messages in and out
- join and leave counts
- handler latency
- outbound queue depth
- slow consumer count
- dropped outbound messages
- payload decode failures
- handler failures
- heartbeat timeout count
- disconnect reason count

Spring Boot 사용자는 `kanal.metrics-enabled=true`일 때 현재 runtime snapshot을 Micrometer meter로 얻고, `kanal.actuator-enabled=true`일 때 counter, active session, joined channel, queue depth, last-seen age를 포함한 `kanal` Actuator endpoint로 조회할 수 있습니다.

## 지금은 의도적으로 제외하는 것

- durable delivery
- replay protocol
- broker adapters
- global ordering
- strong global consistency
- automatic connection migration
- full actor runtime

이 조각들은 나중에 올 수 있습니다. 첫 제품 약속은 JVM을 위한 깨끗하고 typed이며 observable한 realtime application model이어야 합니다.
