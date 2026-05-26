# Roadmap

[한국어](roadmap.ko.md) | English

This roadmap is ordered around the fastest path to a credible realtime developer experience. The near-term goal is not to support every distributed-system feature. It is to make the smallest product slice feel excellent and honest.

## Phase 1: Core Language

Status: foundation implemented, still evolving.

Goals:

- stabilize the channel DSL
- finalize session and context model
- establish presence abstraction
- add channel pattern parsing and matching
- compile channel patterns at registration time
- add registration validation
- define handler invocation contracts
- document what delivery guarantees are and are not provided

Exit criteria:

- `channel<Message>("chat/{roomId}")` can be resolved from `chat/alpha`
- duplicate or ambiguous channel registrations fail clearly
- resolver behavior can be benchmarked independently
- context exposes session, address, parameters, presence, and outbound operations
- tests cover successful and invalid registration cases

## Phase 2: Local Runtime

Status: local frame dispatch, Spring WebSocket adapter, heartbeat lifecycle, bounded queues, and runtime close cleanup started. Fuller operational features remain the next major milestone.

Goals:

- create `kanal-runtime`
- accept WebSocket connections
- decode and encode a small JSON wire protocol
- dispatch join, leave, and message events
- maintain local session and membership indexes
- send heartbeat frames and close heartbeat-timeout sessions
- close the runtime by disconnecting sessions and clearing memberships
- introduce bounded outbound queues
- apply backpressure policies
- expose initial runtime performance metrics

Exit criteria:

- a sample Spring Boot app can run a chat channel locally
- the README example can be exercised through a WebSocket client
- closing a connection cleans up memberships and presence
- slow consumers are handled according to channel policy
- fan-out, queue depth, drops, and handler latency are observable

## Phase 3: Spring Boot Productization

Status: after local runtime.

Goals:

- expose `kanal.endpoint`
- autoconfigure runtime beans
- discover user-defined `RealtimeApplication`
- integrate Jackson message decoding
- add Spring Security principal mapping
- add configuration properties for heartbeat, queues, and metrics
- provide a concise starter guide

Exit criteria:

- a Spring Boot user can add the starter, define one bean, and connect to `/realtime`
- authenticated user identity is available through `SessionDescriptor`
- configuration is documented in English and Korean

## Phase 4: Operational Readiness

Status: Micrometer meter binding, a basic Actuator diagnostics endpoint, slow-consumer signals, disconnect reason tracking, handler failure counters, and payload decode failure counters started. Deeper operator surfaces remain.

Goals:

- Micrometer metrics
- actuator diagnostics
- handler latency measurement
- slow consumer diagnostics
- disconnect reason tracking
- handler and payload failure diagnostics
- structured logs for lifecycle events
- optional JFR integration hooks

Exit criteria:

- operators can answer how many sessions, memberships, drops, slow-consumer signals, handler failures, payload decode failures, disconnects, and heartbeat timeouts exist
- a slow consumer can be identified without attaching a debugger
- runtime state can be inspected through supported APIs

## Phase 5: Samples And Compatibility

Status: before wider release.

Goals:

- chat sample
- presence sample
- authenticated notification sample
- Java interop sample
- Kotlin DSL sample
- compatibility matrix for JDK, Kotlin, Spring Boot, and Jackson versions
- repeatable benchmark scenarios for resolution, local broadcast, slow consumers, heartbeat load, and join/leave churn

Exit criteria:

- new users can understand Kanal by running examples
- maintainers can detect performance regressions
- documentation does not rely only on abstract explanations

## Phase 5.5: Performance Hardening

Status: lightweight benchmark fixture started for channel resolution, local broadcast fan-out, and bounded queue offers.

Goals:

- establish baseline benchmark numbers
- keep the executable benchmark fixture repeatable in CI
- measure memory retained per idle session
- measure p50, p95, and p99 local broadcast latency
- measure allocation rate under fan-out
- validate bounded queue behavior under slow consumers
- tune channel resolver and membership indexes based on data

Exit criteria:

- performance claims are backed by repeatable benchmarks
- Kanal can explain where time and memory go under load
- tuning work does not change the public application model

## Phase 6: Cluster Awareness

Status: Redis metadata model, keyspace helpers, TTL options, and store contract skeleton started.

Goals:

- node registry
- Redis-backed metadata propagation
- user-to-node routing
- cross-node room broadcast
- replicated presence metadata
- failure and timeout semantics

Exit criteria:

- a message published on one node can reach sessions owned by another node
- presence can list local and remote entries
- node failure clears or expires remote metadata predictably
- docs explain consistency and delivery tradeoffs plainly

## Phase 7: Hardening

Status: ongoing after first adoption.

Goals:

- reconnect storm behavior
- load and soak tests
- memory pressure scenarios
- API stability policy
- release automation
- migration guides
- issue templates and contribution guide

Exit criteria:

- Kanal can support early production experiments
- breaking changes are intentional and documented
- operational failure modes are known rather than surprising

## Product Bet

Kanal wins if JVM teams can say:

> We could build this with raw WebSocket, but Kanal gives us the application model, presence, lifecycle, and diagnostics we would otherwise have to invent ourselves.
