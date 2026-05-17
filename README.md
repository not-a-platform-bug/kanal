# Kanal

[한국어](README.ko.md) | English

Application-model-first realtime channels for JVM teams.

Kanal is a Kotlin-first realtime framework for teams that want a higher-level model than raw WebSocket handlers, STOMP destinations, or hand-rolled Redis pub/sub glue. It borrows the product shape of Phoenix Channels while leaning into JVM strengths: Spring Boot integration, Loom-friendly execution, typed application code, and production diagnostics.

The goal is simple: make realtime features feel like normal application code.

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

## Why Kanal

Most JVM stacks can open WebSocket connections, but product teams still rebuild the same realtime model again and again:

- room and topic membership
- join and leave lifecycle hooks
- user-to-session tracking
- presence metadata
- local and cross-node broadcast
- slow-consumer handling
- reconnect and heartbeat behavior
- metrics that explain what is happening in production

Kanal aims to turn those recurring pieces into one coherent application language.

## Product Direction

Kanal is not trying to be a lower-level socket toolkit. It is trying to become the realtime application layer that JVM teams can put above their transport stack.

The first version should be excellent at single-node ergonomics:

- define channels through a small typed DSL
- match paths such as `chat/{roomId}` to concrete channel addresses
- run join, leave, and message handlers in straightforward Kotlin
- track presence as a built-in concept
- deliver outbound messages through bounded queues
- expose useful operational signals from day one

Cluster support comes later, but the design already has an important constraint: sockets stay local to the node that accepted them. Kanal should replicate metadata and route messages, not pretend that live TCP connections can move around the cluster.

## Design Principles

- `channel` is the unit of realtime behavior.
- `presence` is built in, not bolted on later.
- session logic should read like straightforward application code.
- single-node developer experience comes first.
- cluster-aware routing should replicate metadata, not sockets.
- Loom-friendly execution should be possible without leaking runtime tricks into every API.
- metrics and diagnostics are product features, not afterthoughts.

## Module Layout

- `kanal-core`: channel, session, presence, and DSL abstractions
- `kanal-runtime`: runtime foundations for channel resolution metrics, membership indexes, bounded outbound queues, and local runtime counters
- `kanal-spring-boot-starter`: Spring Boot autoconfiguration and integration entrypoint
- `kanal-samples:chat-presence`: detailed Spring Boot sample for chat and presence modeling

Planned modules:

- transport runtime: WebSocket connection lifecycle, frame dispatch, heartbeat, and graceful shutdown
- `kanal-cluster-redis`: Redis-backed metadata propagation and cross-node broadcast

## Current Status

This repository currently contains the first project foundation:

- core DSL for channel registration
- compiled channel pattern matching and channel resolution
- in-memory presence store
- runtime foundations for membership indexes, bounded outbound queues, and metrics
- Spring Boot starter shell
- a detailed chat and presence sample
- architecture and roadmap docs

The first runtime implementation is intentionally next. The most valuable immediate milestone is a local runtime that can accept WebSocket connections, resolve channel patterns, dispatch events, and make the README example truly executable.

## Documentation

- [Product strategy](docs/product-strategy.md)
- [Architecture](docs/architecture.md)
- [Performance strategy](docs/performance.md)
- [Roadmap](docs/roadmap.md)
- [Chat presence example](docs/examples/chat-presence.md)
- [한국어 문서](README.ko.md)

## Non-Goals For The First Runtime

- durable delivery guarantees
- global strong consistency
- automatic connection migration
- full actor runtime
- custom message broker
