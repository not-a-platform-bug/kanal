# Kanal

한국어 | [English](README.md)

JVM 팀을 위한 애플리케이션 모델 중심 realtime channel 프레임워크.

Kanal은 raw WebSocket handler, STOMP destination, 직접 조립한 Redis pub/sub 코드보다 한 단계 높은 realtime 애플리케이션 모델을 제공하려는 Kotlin-first 프레임워크입니다. Phoenix Channels의 제품 감각을 JVM 생태계에 맞게 가져오되, Spring Boot 통합, Loom 친화적인 실행 모델, 타입이 있는 애플리케이션 코드, 운영 진단 가능성을 핵심 강점으로 삼습니다.

목표는 단순합니다. realtime 기능이 평범한 애플리케이션 코드처럼 읽히고 작성되게 만드는 것입니다.

```kotlin
import io.github.kimseungjin.kanal.core.dsl.channel
import io.github.kimseungjin.kanal.core.dsl.realtime

val app =
    realtime {
        channel<ChatMessage>("chat/{roomId}") {
            description("Realtime chat channel")

            onJoin {
                presence.track(
                    key = session.userId ?: session.id,
                    metadata = mapOf("roomId" to address.parameters.getValue("roomId")),
                )
            }

            onMessage { message ->
                broadcast(message)
            }
        }
    }
```

## 왜 Kanal인가

대부분의 JVM 스택은 WebSocket 연결을 열 수 있습니다. 하지만 제품 팀은 여전히 같은 realtime 모델을 매번 직접 조립합니다.

- room과 topic membership
- join과 leave lifecycle hook
- user-to-session 추적
- presence metadata
- 단일 노드와 클러스터 broadcast
- slow consumer 처리
- reconnect와 heartbeat 동작
- 운영 환경에서 상태를 설명해주는 metrics

Kanal은 이 반복되는 조각들을 하나의 일관된 애플리케이션 언어로 끌어올리려는 프로젝트입니다.

## 제품 방향

Kanal은 낮은 수준의 socket toolkit이 되려는 프로젝트가 아닙니다. JVM 팀이 transport stack 위에 올려 사용할 수 있는 realtime application layer가 되는 것이 목표입니다.

첫 번째 버전은 single-node 개발 경험을 탁월하게 만드는 데 집중해야 합니다.

- 작은 typed DSL로 channel 정의
- `chat/{roomId}` 같은 pattern을 실제 channel address로 해석
- join, leave, message handler를 자연스러운 Kotlin 코드로 실행
- presence를 기본 개념으로 제공
- bounded queue를 통한 outbound message 전달
- 초기부터 유용한 운영 신호 제공

클러스터 지원은 이후 단계입니다. 다만 설계의 중요한 제약은 이미 분명합니다. socket은 연결을 수락한 node에 그대로 남아야 합니다. Kanal은 live TCP connection을 이동시키는 척하지 않고, metadata를 복제하고 message를 routing해야 합니다.

## 설계 원칙

- `channel`은 realtime behavior의 기본 단위입니다.
- `presence`는 나중에 덧붙이는 기능이 아니라 기본 기능입니다.
- session logic은 평범한 애플리케이션 코드처럼 읽혀야 합니다.
- single-node developer experience가 먼저입니다.
- cluster-aware routing은 socket이 아니라 metadata를 복제해야 합니다.
- Loom 친화적인 실행 모델은 가능해야 하지만, 모든 public API에 runtime trick이 새어 나오면 안 됩니다.
- metrics와 diagnostics는 부가 기능이 아니라 제품 기능입니다.

## 모듈 구성

- `kanal-core`: channel, session, presence, DSL abstraction
- `kanal-runtime`: channel resolution metrics, membership index, bounded outbound queue, local runtime counter를 위한 runtime foundation
- `kanal-spring-boot-starter`: Spring Boot autoconfiguration과 integration entrypoint
- `kanal-samples:chat-presence`: chat과 presence modeling을 보여주는 자세한 Spring Boot sample

계획 중인 모듈:

- transport runtime: WebSocket connection lifecycle, frame dispatch, heartbeat, graceful shutdown
- `kanal-cluster-redis`: Redis 기반 metadata propagation과 cross-node broadcast

## 현재 상태

이 저장소는 현재 첫 번째 project foundation을 담고 있습니다.

- channel registration을 위한 core DSL
- compiled channel pattern matching과 channel resolution
- in-memory presence store
- membership index, bounded outbound queue, metrics를 위한 runtime foundation
- Spring Boot starter shell
- 자세한 chat and presence sample
- architecture와 roadmap 문서

첫 번째 runtime 구현이 다음 단계입니다. 지금 가장 가치 있는 milestone은 WebSocket 연결을 받고, channel pattern을 해석하고, event를 dispatch해서 README의 예제가 실제로 동작하게 만드는 local runtime입니다.

## 문서

- [제품 전략](docs/product-strategy.ko.md)
- [아키텍처](docs/architecture.ko.md)
- [성능 전략](docs/performance.ko.md)
- [로드맵](docs/roadmap.ko.md)
- [Chat presence 예제](docs/examples/chat-presence.ko.md)
- [English documentation](README.md)

## 첫 runtime의 비목표

- durable delivery guarantee
- global strong consistency
- automatic connection migration
- full actor runtime
- custom message broker
