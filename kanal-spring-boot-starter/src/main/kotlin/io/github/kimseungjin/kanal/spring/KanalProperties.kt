package io.github.kimseungjin.kanal.spring

import org.springframework.boot.context.properties.ConfigurationProperties
import io.github.kimseungjin.kanal.runtime.RuntimeHandlerExecution
import java.time.Duration

@ConfigurationProperties("kanal")
data class KanalProperties(
    var endpoint: String = "/realtime",
    var heartbeatInterval: Duration = Duration.ofSeconds(30),
    var heartbeatTimeout: Duration = Duration.ofSeconds(90),
    var metricsEnabled: Boolean = true,
    var actuatorEnabled: Boolean = true,
    var outboundQueueCapacity: Int = 256,
    var maxTextMessageBufferSize: Int = 64 * 1024,
    var maxBinaryMessageBufferSize: Int = 64 * 1024,
    var maxSessionIdleTimeout: Duration = Duration.ofMinutes(5),
    var asyncSendTimeout: Duration = Duration.ofSeconds(10),
    var handlerExecution: RuntimeHandlerExecution = RuntimeHandlerExecution.DIRECT,
    var virtualThreadNamePrefix: String = "kanal-handler",
)
