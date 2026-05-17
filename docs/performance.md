# Performance Strategy

[한국어](performance.ko.md) | English

Kanal should be fast in the places realtime applications actually break: channel resolution, membership lookup, fan-out, outbound queues, slow consumers, heartbeat load, serialization, and cluster metadata propagation.

The goal is not to chase synthetic throughput before the runtime exists. The goal is to design the runtime so performance is measurable, explainable, and hard to accidentally ruin.

## Performance Principles

- Make the hot path explicit.
- Keep connection ownership local.
- Prefer indexed lookup over repeated scans.
- Bound memory by default.
- Treat slow consumers as a normal runtime condition.
- Measure queue pressure and latency, not only message count.
- Optimize allocation only after the data model is stable, but avoid obviously allocation-heavy APIs in the hot path.
- Define benchmark scenarios before tuning.

## Hot Path Model

The local runtime hot path should be:

1. receive frame
2. decode envelope
3. resolve channel address
4. decode typed payload
5. invoke handler
6. publish outbound message
7. find target sessions from membership index
8. enqueue outbound frames
9. flush to socket

Each stage should be independently measurable. A future performance regression should be attributable to a stage rather than hidden behind a single "messages per second" number.

## Channel Resolution

Incoming channel strings such as `chat/alpha` should not be resolved by scanning every registered channel on every frame.

Direction:

- compile `ChannelPattern` once during application startup
- split patterns into path segments
- route static prefixes before dynamic parameters
- detect ambiguous patterns at registration time
- cache successful concrete channel resolutions when useful

The first resolver can be simple, but it should have the right shape: startup work is acceptable, per-message pattern work should be minimal.

## Membership Indexes

Broadcast performance depends more on membership indexes than on WebSocket plumbing.

The runtime should maintain at least two local indexes:

- `ChannelAddress -> SessionId set`
- `SessionId -> ChannelAddress set`

The first index makes broadcast cheap. The second index makes disconnect cleanup cheap.

Design notes:

- membership and presence must remain separate
- channel address keys should be stable and cheap to compare
- cleanup must be idempotent
- read paths should avoid copying large sets unless isolation requires it

## Fan-Out And Outbound Queues

Realtime workloads often have small inbound traffic and large outbound fan-out. Kanal should model fan-out as a first-class cost.

Direction:

- each connection should have a bounded outbound queue
- broadcast should enqueue per target session without blocking the entire runtime indefinitely
- queue overflow must follow the channel backpressure policy
- metrics should report queue depth, drops, and disconnects by policy

Important distinction:

- inbound handler latency measures application work
- outbound queue pressure measures client delivery health

Both are needed.

## Backpressure Policy Semantics

Existing policies:

- `SUSPEND`
- `DROP_OLDEST`
- `DROP_LATEST`
- `DISCONNECT`

Performance-sensitive semantics:

- `SUSPEND` should be used carefully because it can couple producer speed to slow clients.
- `DROP_OLDEST` is useful for state updates where the newest value matters most.
- `DROP_LATEST` is useful when preserving earlier messages is more important than freshness.
- `DISCONNECT` is useful when slow consumers are considered unhealthy clients.

The runtime should make policy effects visible through metrics and logs.

## Serialization

Typed messages are a product feature, but serialization can become a major cost.

Direction:

- decode the envelope before decoding typed payloads
- keep the envelope small and stable
- avoid double serialization during broadcast when the same payload is sent to many sessions
- allow future codec abstraction without forcing it into the first public API

The Spring starter can begin with Jackson because it is familiar, but the runtime should not become impossible to optimize for other codecs later.

## Heartbeat And Reconnect Load

Heartbeat traffic can become a self-inflicted load spike.

Direction:

- make heartbeat interval configurable
- jitter server-side heartbeat scheduling where appropriate
- track heartbeat timeout counts
- avoid global synchronized heartbeat loops over all sessions
- define reconnect storm behavior before cluster support

The runtime should assume that many clients may reconnect at the same time after a deploy, network flap, or mobile wake-up event.

## Concurrency Model

Kanal should use JVM strengths without exposing them awkwardly.

Direction:

- keep transport I/O on event-loop style infrastructure
- execute user handlers in a blocking-friendly model
- leave room for virtual-thread execution
- avoid requiring users to write callback-heavy handler code
- keep mutable runtime state behind clear ownership boundaries

The public API should not require users to know which thread model is active.

## Memory Budget

The first runtime should be explicit about memory-bearing structures:

- session registry
- membership indexes
- presence entries
- outbound queues
- decoded frames
- serialized outbound payloads

Defaults should be bounded. Unbounded queues are not a friendly developer experience; they are delayed production failures.

## Metrics For Performance Work

Minimum useful metrics:

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

Useful benchmark outputs:

- p50, p95, p99 handler dispatch latency
- p50, p95, p99 end-to-end local broadcast latency
- allocation rate under broadcast
- memory retained per idle session
- maximum stable fan-out for a fixed queue policy

## Benchmark Scenarios

Kanal should eventually maintain repeatable benchmarks for:

1. channel resolution with many registered patterns
2. join and leave churn
3. many small rooms
4. one large room
5. slow consumer under each backpressure policy
6. presence track/untrack/list under churn
7. heartbeat load with many idle sessions
8. reconnect storm
9. local broadcast with JSON payloads
10. cross-node metadata propagation after cluster support exists

Benchmarks should be treated as design feedback, not marketing numbers.

## Implemented Runtime Foundation

The initial performance-oriented foundation now includes:

1. compiled and validated `ChannelPattern`
2. `ChannelResolver` exposed through `RealtimeApplication.resolve(path)`
3. `MeasuredChannelResolver` for recording resolution latency in `kanal-runtime`
4. local membership indexes in `kanal-runtime`, including fan-out measurement through broadcast target lookup
5. bounded outbound queue behavior for all current backpressure policies
6. minimal runtime metrics for sessions, memberships, frames, drops, disconnects, fan-out, queue depth, resolution latency, and handler latency

This keeps Kanal aligned with its product promise: a realtime application model that is pleasant to write and honest under pressure.
