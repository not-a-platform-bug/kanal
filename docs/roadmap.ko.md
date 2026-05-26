# 로드맵

한국어 | [English](roadmap.md)

이 로드맵은 신뢰할 수 있는 realtime developer experience에 가장 빨리 도달하는 순서로 구성되어 있습니다. 단기 목표는 모든 distributed-system 기능을 지원하는 것이 아닙니다. 가장 작은 제품 단면을 훌륭하고 정직하게 만드는 것입니다.

## Phase 1: Core Language

상태: foundation 구현됨, 계속 진화 중.

목표:

- channel DSL 안정화
- session과 context model 확정
- presence abstraction 정립
- channel pattern parsing과 matching 추가
- registration 시 channel pattern compile
- registration validation 추가
- handler invocation contract 정의
- 제공하는 delivery guarantee와 제공하지 않는 guarantee 문서화

완료 기준:

- `channel<Message>("chat/{roomId}")`가 `chat/alpha`에서 resolve될 수 있음
- 중복되거나 모호한 channel registration이 명확히 실패함
- resolver behavior를 독립적으로 benchmark할 수 있음
- context가 session, address, parameters, presence, outbound operation을 노출함
- 성공/실패 registration case가 test로 보호됨

## Phase 2: Local Runtime

상태: local frame dispatch, Spring WebSocket adapter, heartbeat lifecycle, bounded queue, runtime close cleanup이 시작됨. 더 완전한 운영 기능이 다음 주요 milestone.

목표:

- `kanal-runtime` 생성
- WebSocket connection 수락
- 작은 JSON wire protocol decode/encode
- join, leave, message event dispatch
- local session과 membership index 유지
- heartbeat frame 전송과 heartbeat-timeout session close
- runtime close 시 session disconnect와 membership cleanup
- bounded outbound queue 도입
- backpressure policy 적용
- 초기 runtime performance metrics 노출

완료 기준:

- sample Spring Boot app에서 chat channel을 local로 실행할 수 있음
- README 예제를 WebSocket client로 실제 사용 가능
- connection close 시 membership과 presence가 정리됨
- slow consumer가 channel policy에 따라 처리됨
- fan-out, queue depth, drops, handler latency를 관찰할 수 있음

## Phase 3: Spring Boot Productization

상태: local runtime 이후.

목표:

- `kanal.endpoint` 노출
- runtime bean autoconfiguration
- user-defined `RealtimeApplication` 발견
- Jackson message decoding 통합
- Spring Security principal mapping 추가
- heartbeat, queue, metrics 설정 property 제공
- 간결한 starter guide 제공

완료 기준:

- Spring Boot 사용자가 starter를 추가하고 bean 하나를 정의한 뒤 `/realtime`에 연결할 수 있음
- authenticated user identity가 `SessionDescriptor`를 통해 제공됨
- configuration이 영어와 한국어로 문서화됨

## Phase 4: Operational Readiness

상태: Micrometer meter binding, 기본 Actuator diagnostics endpoint, slow-consumer signal, disconnect reason tracking, handler failure counter, payload decode failure counter 시작됨. 더 깊은 운영 표면은 아직 남아 있음.

목표:

- Micrometer metrics
- actuator diagnostics
- handler latency measurement
- slow consumer diagnostics
- disconnect reason tracking
- handler와 payload failure diagnostics
- lifecycle event structured log
- optional JFR integration hook

완료 기준:

- 운영자가 session, membership, drop, slow-consumer signal, handler failure, payload decode failure, disconnect, heartbeat timeout 수를 답할 수 있음
- debugger 없이 slow consumer를 식별할 수 있음
- runtime state를 supported API로 inspect할 수 있음

## Phase 5: Samples And Compatibility

상태: 더 넓은 release 이전.

목표:

- chat sample
- presence sample
- authenticated notification sample
- Java interop sample
- Kotlin DSL sample
- JDK, Kotlin, Spring Boot, Jackson version compatibility matrix
- resolution, local broadcast, slow consumers, heartbeat load, join/leave churn에 대한 반복 가능한 benchmark scenario

완료 기준:

- 신규 사용자가 예제를 실행하며 Kanal을 이해할 수 있음
- maintainer가 performance regression을 감지할 수 있음
- 문서가 추상적인 설명에만 의존하지 않음

## Phase 5.5: Performance Hardening

상태: channel resolution, local broadcast fan-out, bounded queue offer를 위한 lightweight benchmark fixture 시작됨.

목표:

- baseline benchmark number 수립
- executable benchmark fixture를 CI에서 반복 가능하게 유지
- idle session당 retained memory 측정
- p50, p95, p99 local broadcast latency 측정
- fan-out 상황의 allocation rate 측정
- slow consumer 상황에서 bounded queue behavior 검증
- data 기반으로 channel resolver와 membership index tuning

완료 기준:

- performance claim이 반복 가능한 benchmark로 뒷받침됨
- Kanal이 load 상황에서 시간과 memory가 어디에 쓰이는지 설명할 수 있음
- tuning work가 public application model을 바꾸지 않음

## Phase 6: Cluster Awareness

상태: Redis metadata model, keyspace helper, TTL option, store contract skeleton 시작됨.

목표:

- node registry
- Redis-backed metadata propagation
- user-to-node routing
- cross-node room broadcast
- replicated presence metadata
- failure와 timeout semantics

완료 기준:

- 한 node에서 publish한 message가 다른 node 소유 session에 도달할 수 있음
- presence가 local과 remote entry를 함께 list할 수 있음
- node failure 시 remote metadata가 예측 가능하게 clear 또는 expire됨
- docs가 consistency와 delivery tradeoff를 솔직하게 설명함

## Phase 7: Hardening

상태: 첫 adoption 이후 계속.

목표:

- reconnect storm behavior
- load and soak tests
- memory pressure scenario
- API stability policy
- release automation
- migration guides
- issue template과 contribution guide

완료 기준:

- Kanal이 early production experiment를 지원할 수 있음
- breaking change가 의도적이고 문서화됨
- operational failure mode가 놀라움이 아니라 알려진 선택지가 됨

## 제품적 베팅

Kanal이 이기는 순간은 JVM 팀이 이렇게 말할 수 있을 때입니다.

> raw WebSocket으로도 만들 수 있지만, Kanal은 우리가 직접 발명해야 했던 application model, presence, lifecycle, diagnostics를 이미 제공한다.
