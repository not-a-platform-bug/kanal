package io.github.kimseungjin.kanal.runtime

import java.time.Duration

data class RealtimeRuntimeOptions(
    val outboundQueueCapacity: Int = 256,
    val eventLogCapacity: Int = 256,
    val heartbeatInterval: Duration = Duration.ofSeconds(30),
    val heartbeatTimeout: Duration = Duration.ofSeconds(90),
    val handlerExecution: RuntimeHandlerExecution = RuntimeHandlerExecution.DIRECT,
    val virtualThreadNamePrefix: String = "kanal-handler",
) {
    init {
        require(outboundQueueCapacity > 0) { "Outbound queue capacity must be greater than zero" }
        require(eventLogCapacity > 0) { "Runtime event log capacity must be greater than zero" }
        require(!heartbeatInterval.isNegative) { "Heartbeat interval must not be negative" }
        require(!heartbeatTimeout.isNegative && !heartbeatTimeout.isZero) { "Heartbeat timeout must be greater than zero" }
        require(heartbeatInterval.isZero || heartbeatTimeout >= heartbeatInterval) {
            "Heartbeat timeout must be greater than or equal to heartbeat interval"
        }
        require(virtualThreadNamePrefix.isNotBlank()) { "Virtual thread name prefix must not be blank" }
    }
}
