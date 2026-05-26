# Architecture

[한국어](architecture.ko.md) | English

Kanal is an application-model-first realtime framework. The architecture is organized around the concepts application teams use when they build realtime features: channels, sessions, presence, membership, dispatch, outbound delivery, and diagnostics.

## Layering

Kanal should stay layered so that the core model remains stable while runtime and framework integrations evolve.

1. `kanal-core`
   Defines the language of the system: channel definitions, channel patterns, sessions, presence, contexts, message handlers, and backpressure policy.
2. `kanal-runtime`
   Owns runtime foundations such as membership indexes, bounded outbound queues, local metrics, local connection lifecycle, frame decoding, handler dispatch, heartbeat, and runtime shutdown.
3. `kanal-spring-boot-starter`
   Wires the runtime into Spring Boot through autoconfiguration, properties, security integration, metrics, and actuator endpoints.
4. `cluster adapters`
   Replicate metadata such as node presence, user routing, channel membership, and presence state. Sockets remain owned by the node that accepted them.

The core rule is that framework integrations should depend on the model, not define the model.

## Core Concepts

### Channel

A channel is the primary unit of realtime behavior.

Examples:

- `chat/{roomId}`
- `notifications/{userId}`
- `presence/{workspaceId}`

A channel owns:

- path pattern
- message type
- join, leave, and message handlers
- backpressure policy
- optional product-facing metadata such as description

The first runtime needs a resolver that turns an incoming concrete path like `chat/alpha` into a channel definition plus extracted parameters such as `roomId=alpha`.

The current core already compiles channel patterns at registration time and exposes a resolver through `RealtimeApplication.resolve(path)`.

### Session

A session represents one live client connection from the application point of view.

It should contain:

- generated session id
- optional authenticated user id
- principal attributes
- local attributes
- connection metadata useful for diagnostics

A user may have many sessions. Kanal should model that directly instead of assuming one user equals one connection.

### Membership

Membership is the relationship between a session and a channel address. It is separate from presence.

Membership answers:

- Which sessions are subscribed to this channel address?
- Which channel addresses has this session joined?
- What should receive a broadcast?
- What must be cleaned up when a connection closes?

Presence is user-facing state. Membership is runtime routing state.

### Presence

Presence models who or what is currently visible in a channel and attaches lightweight metadata.

The minimal contract is:

- `track`
- `untrack`
- `list`

The in-memory implementation is good for the local runtime. A cluster adapter can later replicate presence metadata without moving socket ownership.

### Context

Handlers receive `ChannelContext`, which should become the stable application-facing surface for:

- session data
- resolved channel address
- extracted path parameters
- presence access
- outbound publishing operations
- tracing and security metadata
- cancellation and timeout signals when needed

The context should stay pleasant to use from Kotlin. Runtime concerns should be available when needed, but not dominate the handler API.

## Local Runtime Direction

The first runtime should optimize for a crisp single-node experience.

Responsibilities:

- accept WebSocket connections
- decode a small Kanal wire protocol
- resolve channel patterns
- dispatch join, leave, and message events
- maintain local membership indexes
- track session lifecycle
- apply bounded outbound queues
- enforce channel backpressure policy
- send heartbeat frames
- shut down gracefully

The runtime can use event-loop transport I/O while running user handlers in a blocking-style model. That keeps the public API friendly to ordinary application code and leaves room for virtual-thread execution.

## Wire Protocol Shape

The initial protocol should be boring and inspectable.

Current frame shape:

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

Expected early events:

- `join`
- `leave`
- `message`
- `heartbeat`
- `error`
- `reply`

Reply payloads are wrapped as:

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

Error payloads use stable codes:

```json
{
  "event": "error",
  "payload": {
    "code": "malformed_frame",
    "message": "Malformed frame"
  }
}
```

Kanal should avoid promising durable delivery in the first runtime. A clear at-most-once local delivery model is easier to reason about than an implied guarantee the system does not actually provide.

The current runtime foundation dispatches `join`, `leave`, `message`, and `heartbeat` frames in this shape. It also sends scheduled heartbeat frames, closes heartbeat-timeout sessions, and clears memberships on runtime close. The Spring Boot starter decodes text WebSocket messages as JSON frames at the default `/realtime` endpoint and hands them to the runtime.

## Backpressure

Backpressure must be explicit because realtime systems fail at the edges first.

Existing policies:

- `SUSPEND`
- `DROP_OLDEST`
- `DROP_LATEST`
- `DISCONNECT`

The runtime should expose metrics for queue depth, dropped messages, disconnects caused by policy, and slow consumers.

## Performance Model

Kanal's performance model should focus on the realtime hot path rather than a single aggregate throughput number.

The local hot path is:

1. receive frame
2. decode envelope
3. resolve channel address
4. decode typed payload
5. invoke handler
6. publish outbound message
7. find target sessions from membership indexes
8. enqueue outbound frames
9. flush to socket

Important design consequences:

- `ChannelPattern` should be compiled and validated at registration time.
- channel resolution should avoid scanning every registered pattern per frame.
- runtime membership should maintain both `ChannelAddress -> SessionId set` and `SessionId -> ChannelAddress set`.
- outbound queues should be bounded by default.
- fan-out size, queue depth, handler latency, and dropped messages should be measured separately.

The initial code foundation now includes these pieces, including the local JSON frame dispatch path and Spring WebSocket adapter.

The `kanal-benchmarks` module provides a lightweight executable fixture for channel resolution, local broadcast fan-out, and bounded queue offer behavior. See the [README](../README.md) for the current command and tested runtime path.

## Cluster Direction

Cluster support must not imply connection migration.

What can replicate:

- node registry
- user-to-node routing metadata
- channel membership metadata
- presence metadata
- cross-node broadcast envelopes

What stays local:

- TCP and WebSocket connections
- outbound queues
- connection heartbeat state
- local session cleanup

This gives Kanal a realistic distributed model: route to the node that owns the connection, replicate enough metadata to know where to route, and keep failure behavior understandable.

The `kanal-cluster-redis` module now starts this shape with metadata data classes, stable Redis keyspace helpers, TTL/options validation, and a `RedisClusterMetadataStore` contract. It intentionally does not claim a working Redis implementation yet.

## Observability

Kanal should treat diagnostics as part of the product.

Early metrics:

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

Spring Boot users get Micrometer meters for the current runtime snapshot when `kanal.metrics-enabled=true`, and a `kanal` Actuator endpoint with counters, active sessions, joined channels, queue depths, and last-seen ages when `kanal.actuator-enabled=true`.

## Deliberately Missing For Now

- durable delivery
- replay protocol
- broker adapters
- global ordering
- strong global consistency
- automatic connection migration
- full actor runtime

Those pieces can come later. The first product promise should be a clean, typed, observable realtime application model for the JVM.
