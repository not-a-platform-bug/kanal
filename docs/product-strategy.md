# Product Strategy

[한국어](product-strategy.ko.md) | English

Kanal should become the realtime application framework JVM teams reach for when raw WebSocket is too low-level and STOMP is too destination-centric.

## Positioning

Kanal is:

- a channel-oriented realtime application framework
- Kotlin-first, but Java-friendly over time
- Spring Boot friendly without making Spring define the core model
- designed for local clarity first and cluster awareness later
- explicit about operational behavior

Kanal is not:

- a generic message broker
- a durable event log
- an actor system
- a global consistency layer
- a replacement for Kafka, Redis, or NATS

The product line should be:

> Phoenix-style realtime channels for JVM applications.

The deeper promise is:

> Realtime behavior should be modeled as application code, not scattered across socket handlers, session maps, and pub/sub glue.

## Target Users

Primary users:

- Kotlin and Spring Boot teams building collaborative or live applications
- backend engineers who need realtime features but do not want to own a custom WebSocket framework
- product teams that need presence, lifecycle hooks, and broadcast semantics quickly

Early use cases:

- chat rooms
- collaborative workspaces
- live notifications
- dashboard updates
- multiplayer-lite application state
- presence-aware SaaS experiences

## The Sharp Edge

The JVM already has WebSocket support. Kanal needs to win above that layer.

The strongest wedge is not connection handling by itself. It is the bundled application model:

- typed channel definitions
- path parameter extraction
- lifecycle hooks
- session identity
- built-in presence
- membership-based broadcast
- backpressure policy
- metrics and diagnostics

If Kanal makes these pieces feel native, it becomes valuable even before clustering exists.

## Performance Point Of View

Kanal should not sell performance as vague speed. It should sell predictable behavior under realtime pressure.

The product should make these questions easy to answer:

- How expensive is channel resolution?
- How many sessions will a broadcast target?
- Which clients are building outbound queue pressure?
- Which backpressure policy caused drops or disconnects?
- How much memory does an idle session retain?

This gives Kanal a practical JVM story: application ergonomics first, but with operational and performance truth built into the runtime.

## MVP Shape

The first impressive demo should be a Spring Boot chat or workspace presence app.

It should show:

- one dependency
- one `RealtimeApplication` bean
- one WebSocket endpoint
- typed message handlers
- automatic presence cleanup
- basic metrics
- readable logs

The demo should avoid overselling. It can be single-node and still be compelling if the application model is crisp.

## API Taste

Kanal APIs should feel:

- small
- typed
- boring in the best way
- explicit about failure behavior
- friendly to Kotlin DSL users
- possible to use from Java where it matters

Avoid making users understand the runtime before they can define a channel.

## Differentiation

Compared with raw WebSocket:

- Kanal provides channel lifecycle, membership, presence, and broadcast.

Compared with STOMP:

- Kanal is less destination-protocol-oriented and more application-model-oriented.

Compared with a custom Redis pub/sub layer:

- Kanal keeps local connection ownership, membership, and diagnostics in one runtime model.

Compared with a full actor runtime:

- Kanal focuses on realtime application ergonomics instead of becoming a general concurrency platform.

## Near-Term Narrative

The story for the next release should be:

1. Define channels in Kotlin.
2. Connect through Spring Boot.
3. Join rooms and exchange typed messages.
4. Track presence automatically.
5. See what the runtime is doing.

That is enough to make the project feel real.

## Documentation Strategy

Every public document should have English and Korean versions.

Naming convention:

- English: `README.md`, `docs/name.md`
- Korean: `README.ko.md`, `docs/name.ko.md`

English should be the default for global open-source distribution. Korean should be first-class, not a short summary. Both versions should carry the same product intent, even if the phrasing is natural for each language.
