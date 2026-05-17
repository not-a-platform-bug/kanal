package io.github.kimseungjin.kanal.spring

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("kanal")
data class KanalProperties(
    var endpoint: String = "/realtime",
    var heartbeatInterval: Duration = Duration.ofSeconds(30),
    var metricsEnabled: Boolean = true,
)
