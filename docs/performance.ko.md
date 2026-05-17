# 성능 전략

한국어 | [English](performance.md)

Kanal은 realtime application이 실제로 깨지는 지점에서 빨라야 합니다. 그 지점은 channel resolution, membership lookup, fan-out, outbound queue, slow consumer, heartbeat load, serialization, cluster metadata propagation입니다.

Runtime이 아직 없는 상태에서 synthetic throughput 숫자를 먼저 좇는 것은 적절하지 않습니다. 지금 목표는 성능을 측정 가능하고, 설명 가능하고, 실수로 망치기 어렵게 만드는 runtime 구조를 잡는 것입니다.

## 성능 원칙

- hot path를 명시적으로 만든다.
- connection ownership은 local에 둔다.
- 반복 scan보다 indexed lookup을 우선한다.
- memory는 기본적으로 bounded하게 둔다.
- slow consumer를 예외가 아니라 일반적인 runtime condition으로 다룬다.
- message count만 보지 않고 queue pressure와 latency를 함께 측정한다.
- data model이 안정되기 전까지 과한 allocation 최적화는 피하되, hot path에 명백히 allocation-heavy한 API는 만들지 않는다.
- tuning 전에 benchmark scenario를 정의한다.

## Hot Path Model

Local runtime의 hot path는 다음 흐름이어야 합니다.

1. frame 수신
2. envelope decode
3. channel address resolve
4. typed payload decode
5. handler invoke
6. outbound message publish
7. membership index에서 target session 조회
8. outbound frame enqueue
9. socket flush

각 단계는 독립적으로 측정 가능해야 합니다. 나중에 performance regression이 생겼을 때 단순한 "messages per second" 숫자 뒤에 숨어 있지 않고 어느 단계가 문제인지 알 수 있어야 합니다.

## Channel Resolution

`chat/alpha` 같은 incoming channel string을 매 frame마다 모든 registered channel과 비교해서 resolve하면 안 됩니다.

방향:

- application startup 시 `ChannelPattern`을 한 번 compile
- pattern을 path segment로 분리
- dynamic parameter보다 static prefix를 먼저 routing
- registration 시 ambiguous pattern 감지
- 유용한 경우 concrete channel resolution cache 적용

첫 resolver는 단순해도 됩니다. 하지만 형태는 맞아야 합니다. Startup work는 괜찮지만, per-message pattern work는 최소화해야 합니다.

## Membership Indexes

Broadcast 성능은 WebSocket plumbing보다 membership index에 더 크게 좌우됩니다.

Runtime은 최소 두 개의 local index를 유지해야 합니다.

- `ChannelAddress -> SessionId set`
- `SessionId -> ChannelAddress set`

첫 번째 index는 broadcast를 싸게 만듭니다. 두 번째 index는 disconnect cleanup을 싸게 만듭니다.

설계 메모:

- membership과 presence는 분리되어야 한다.
- channel address key는 stable하고 비교 비용이 낮아야 한다.
- cleanup은 idempotent해야 한다.
- read path는 isolation이 꼭 필요한 경우가 아니라면 큰 set copy를 피해야 한다.

## Fan-Out And Outbound Queues

Realtime workload는 inbound traffic은 작고 outbound fan-out은 큰 경우가 많습니다. Kanal은 fan-out을 first-class cost로 모델링해야 합니다.

방향:

- 각 connection은 bounded outbound queue를 가져야 한다.
- broadcast는 target session별로 enqueue하되 전체 runtime을 무기한 block하면 안 된다.
- queue overflow는 channel backpressure policy를 따라야 한다.
- metrics는 queue depth, drops, policy로 인한 disconnect를 보여줘야 한다.

중요한 구분:

- inbound handler latency는 application work를 측정한다.
- outbound queue pressure는 client delivery health를 측정한다.

둘 다 필요합니다.

## Backpressure Policy Semantics

기존 policy:

- `SUSPEND`
- `DROP_OLDEST`
- `DROP_LATEST`
- `DISCONNECT`

성능 관점의 의미:

- `SUSPEND`는 producer speed가 slow client와 결합될 수 있으므로 신중히 써야 한다.
- `DROP_OLDEST`는 최신 값이 가장 중요한 state update에 유용하다.
- `DROP_LATEST`는 freshness보다 앞선 message 보존이 중요할 때 유용하다.
- `DISCONNECT`는 slow consumer를 unhealthy client로 보는 경우 유용하다.

Runtime은 policy 효과를 metrics와 log로 볼 수 있게 해야 합니다.

## Serialization

Typed message는 제품 기능이지만 serialization은 큰 비용이 될 수 있습니다.

방향:

- typed payload decode 전에 envelope를 먼저 decode
- envelope는 작고 안정적으로 유지
- 같은 payload를 많은 session에 broadcast할 때 double serialization 회피
- 첫 public API에 과하게 드러내지 않되 future codec abstraction 여지 확보

Spring starter는 familiar한 Jackson으로 시작할 수 있습니다. 하지만 runtime이 나중에 다른 codec으로 최적화하기 어려운 구조가 되면 안 됩니다.

## Heartbeat And Reconnect Load

Heartbeat traffic은 스스로 만든 load spike가 될 수 있습니다.

방향:

- heartbeat interval configurable
- 적절한 경우 server-side heartbeat scheduling에 jitter 적용
- heartbeat timeout count 추적
- 모든 session을 한 번에 도는 global synchronized heartbeat loop 회피
- cluster support 이전에 reconnect storm behavior 정의

Runtime은 deploy, network flap, mobile wake-up 이후 많은 client가 동시에 reconnect할 수 있다고 가정해야 합니다.

## Concurrency Model

Kanal은 JVM의 강점을 사용하되 사용자에게 어색하게 노출하면 안 됩니다.

방향:

- transport I/O는 event-loop style infrastructure에 둔다.
- user handler는 blocking-friendly model로 실행한다.
- virtual-thread execution 여지를 남긴다.
- 사용자가 callback-heavy handler code를 쓰도록 강제하지 않는다.
- mutable runtime state는 명확한 ownership boundary 뒤에 둔다.

Public API는 현재 어떤 thread model이 active인지 사용자가 알아야만 쓸 수 있는 형태가 되면 안 됩니다.

## Memory Budget

첫 runtime은 memory를 갖는 구조를 명시해야 합니다.

- session registry
- membership indexes
- presence entries
- outbound queues
- decoded frames
- serialized outbound payloads

Default는 bounded해야 합니다. Unbounded queue는 친절한 developer experience가 아니라 지연된 production failure입니다.

## 성능 작업을 위한 Metrics

최소한 유용한 metrics:

- active sessions
- active memberships
- inbound frames
- outbound frames
- broadcast fan-out size
- handler latency
- channel resolution latency
- outbound queue depth
- dropped outbound messages
- disconnects by backpressure policy
- heartbeat timeouts
- serialization failures

유용한 benchmark output:

- p50, p95, p99 handler dispatch latency
- p50, p95, p99 end-to-end local broadcast latency
- broadcast 중 allocation rate
- idle session당 retained memory
- fixed queue policy에서 안정적으로 감당 가능한 최대 fan-out

## Benchmark Scenarios

Kanal은 eventually 반복 가능한 benchmark를 유지해야 합니다.

1. 많은 registered pattern에서 channel resolution
2. join and leave churn
3. many small rooms
4. one large room
5. 각 backpressure policy에서 slow consumer
6. churn 상황의 presence track/untrack/list
7. 많은 idle session에서 heartbeat load
8. reconnect storm
9. JSON payload local broadcast
10. cluster support 이후 cross-node metadata propagation

Benchmark는 marketing number가 아니라 design feedback으로 다뤄야 합니다.

## 구현된 Runtime Foundation

초기 성능 foundation에는 다음 조각들이 들어와 있습니다.

1. compile and validate되는 `ChannelPattern`
2. `RealtimeApplication.resolve(path)`를 통해 노출되는 `ChannelResolver`
3. `kanal-runtime`에서 resolution latency를 기록하는 `MeasuredChannelResolver`
4. broadcast target lookup을 통한 fan-out measurement를 포함하는 `kanal-runtime`의 local membership indexes
5. 현재 backpressure policy 전체에 대한 bounded outbound queue behavior
6. session, membership, frame, drop, disconnect, fan-out, queue depth, resolution latency, handler latency를 위한 최소 runtime metrics

이 방향은 Kanal의 제품 약속과 맞습니다. 작성하기 즐겁고, 압박이 걸렸을 때도 정직한 realtime application model을 만드는 것입니다.
