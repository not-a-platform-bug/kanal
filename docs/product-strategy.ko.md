# 제품 전략

한국어 | [English](product-strategy.md)

Kanal은 raw WebSocket이 너무 낮은 수준이고 STOMP가 destination 중심으로 느껴질 때, JVM 팀이 선택하는 realtime application framework가 되어야 합니다.

## 포지셔닝

Kanal은 이런 프로젝트입니다.

- channel 중심 realtime application framework
- Kotlin-first, 시간이 지나며 Java-friendly
- Spring Boot 친화적이지만 Spring이 core model을 정의하지 않음
- local clarity를 먼저 만들고 이후 cluster awareness로 확장
- operational behavior를 명시적으로 다룸

Kanal은 이런 프로젝트가 아닙니다.

- generic message broker
- durable event log
- actor system
- global consistency layer
- Kafka, Redis, NATS의 대체재

제품 문장은 이렇게 가져갈 수 있습니다.

> Phoenix-style realtime channels for JVM applications.

더 깊은 약속은 이것입니다.

> Realtime behavior는 socket handler, session map, pub/sub glue 여기저기에 흩어지는 것이 아니라 애플리케이션 코드로 모델링되어야 한다.

## 대상 사용자

주요 사용자:

- collaborative 또는 live application을 만드는 Kotlin/Spring Boot 팀
- realtime 기능이 필요하지만 custom WebSocket framework를 직접 소유하고 싶지 않은 backend engineer
- presence, lifecycle hook, broadcast semantics가 빠르게 필요한 product team

초기 use case:

- chat rooms
- collaborative workspaces
- live notifications
- dashboard updates
- multiplayer-lite application state
- presence-aware SaaS experiences

## 날카로운 차별점

JVM에는 이미 WebSocket support가 있습니다. Kanal은 그 위의 layer에서 이겨야 합니다.

가장 강한 진입점은 connection handling 자체가 아닙니다. 함께 제공되는 application model입니다.

- typed channel definitions
- path parameter extraction
- lifecycle hooks
- session identity
- built-in presence
- membership-based broadcast
- backpressure policy
- metrics and diagnostics

이 조각들이 native하게 느껴지면, cluster가 없더라도 Kanal은 충분히 가치 있습니다.

## 성능 관점

Kanal은 막연한 빠름을 성능으로 팔면 안 됩니다. Realtime pressure 아래에서 예측 가능한 behavior를 제공해야 합니다.

제품은 이런 질문에 쉽게 답할 수 있어야 합니다.

- channel resolution은 얼마나 비싼가?
- broadcast는 몇 session을 target하는가?
- 어떤 client가 outbound queue pressure를 만들고 있는가?
- 어떤 backpressure policy 때문에 drop 또는 disconnect가 발생했는가?
- idle session 하나가 memory를 얼마나 유지하는가?

이 관점은 Kanal에 실용적인 JVM story를 줍니다. Application ergonomics를 먼저 제공하되, runtime 안에 operational truth와 performance truth를 함께 넣는 것입니다.

## MVP 형태

첫 인상 좋은 demo는 Spring Boot chat 또는 workspace presence app이어야 합니다.

보여줘야 할 것:

- dependency 하나
- `RealtimeApplication` bean 하나
- WebSocket endpoint 하나
- typed message handler
- automatic presence cleanup
- basic metrics
- readable logs

Demo는 과장하면 안 됩니다. Single-node여도 application model이 선명하면 충분히 매력적입니다.

## API 취향

Kanal API는 이런 느낌이어야 합니다.

- 작다
- typed하다
- 좋은 의미로 boring하다
- failure behavior가 명시적이다
- Kotlin DSL 사용자에게 자연스럽다
- 중요한 지점에서는 Java에서도 사용할 수 있다

사용자가 channel 하나를 정의하기 전에 runtime을 이해해야 하는 구조는 피해야 합니다.

## 차별화

Raw WebSocket과 비교하면:

- Kanal은 channel lifecycle, membership, presence, broadcast를 제공합니다.

STOMP와 비교하면:

- Kanal은 destination protocol보다 application model에 집중합니다.

직접 만든 Redis pub/sub layer와 비교하면:

- Kanal은 local connection ownership, membership, diagnostics를 하나의 runtime model로 묶습니다.

Full actor runtime과 비교하면:

- Kanal은 general concurrency platform이 되기보다 realtime application ergonomics에 집중합니다.

## 가까운 시기의 이야기

다음 release의 이야기는 이렇게 단순해야 합니다.

1. Kotlin으로 channel을 정의한다.
2. Spring Boot로 연결한다.
3. Room에 join하고 typed message를 주고받는다.
4. Presence를 자동으로 추적한다.
5. Runtime이 무엇을 하는지 볼 수 있다.

이 정도면 프로젝트가 충분히 현실감 있게 느껴집니다.

## 문서 전략

모든 public document는 영어와 한국어 버전을 가져야 합니다.

Naming convention:

- English: `README.md`, `docs/name.md`
- Korean: `README.ko.md`, `docs/name.ko.md`

Global open-source distribution을 위해 영어를 default로 둡니다. 하지만 한국어판은 짧은 요약이 아니라 first-class document여야 합니다. 두 버전은 각 언어에 자연스러운 문장으로 같은 product intent를 담아야 합니다.
