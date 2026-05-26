package io.github.kimseungjin.kanal.spring

import io.github.kimseungjin.kanal.core.InMemoryPresenceStore
import io.github.kimseungjin.kanal.core.PresenceStore
import io.github.kimseungjin.kanal.core.RealtimeApplication
import io.github.kimseungjin.kanal.core.dsl.realtime
import io.github.kimseungjin.kanal.runtime.LocalRealtimeRuntime
import io.github.kimseungjin.kanal.runtime.RealtimeRuntimeOptions
import io.github.kimseungjin.kanal.runtime.RuntimeMetrics
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket

@AutoConfiguration
@EnableWebSocket
@EnableConfigurationProperties(KanalProperties::class)
class KanalAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun kanalPresenceStore(): PresenceStore = InMemoryPresenceStore()

    @Bean
    @ConditionalOnMissingBean
    fun kanalRealtimeApplication(): RealtimeApplication = realtime {}

    @Bean
    @ConditionalOnMissingBean
    fun kanalRuntimeMetrics(): RuntimeMetrics = RuntimeMetrics()

    @Bean
    @ConditionalOnMissingBean
    fun kanalLocalRealtimeRuntime(
        realtimeApplication: RealtimeApplication,
        presenceStore: PresenceStore,
        properties: KanalProperties,
        runtimeMetrics: RuntimeMetrics,
    ): LocalRealtimeRuntime =
        LocalRealtimeRuntime(
            application = realtimeApplication,
            presenceStore = presenceStore,
            codec = JacksonRuntimePayloadCodec.default(),
            options =
                RealtimeRuntimeOptions(
                    outboundQueueCapacity = properties.outboundQueueCapacity,
                    heartbeatInterval = properties.heartbeatInterval,
                    heartbeatTimeout = properties.heartbeatTimeout,
                    handlerExecution = properties.handlerExecution,
                    virtualThreadNamePrefix = properties.virtualThreadNamePrefix,
                ),
            metrics = runtimeMetrics,
        )

    @Bean
    @ConditionalOnClass(WebSocketHandler::class)
    @ConditionalOnMissingBean
    fun kanalWebSocketHandler(localRealtimeRuntime: LocalRealtimeRuntime): KanalWebSocketHandler =
        KanalWebSocketHandler(localRealtimeRuntime)

    @Bean
    @ConditionalOnClass(WebSocketHandler::class)
    @ConditionalOnMissingBean
    fun kanalWebSocketConfiguration(
        properties: KanalProperties,
        kanalWebSocketHandler: KanalWebSocketHandler,
    ): KanalWebSocketConfiguration =
        KanalWebSocketConfiguration(properties, kanalWebSocketHandler)

    @Bean
    @ConditionalOnClass(MeterBinder::class)
    @ConditionalOnProperty(prefix = "kanal", name = ["metrics-enabled"], havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = ["kanalRuntimeMeterBinder"])
    fun kanalRuntimeMeterBinder(runtimeMetrics: RuntimeMetrics): MeterBinder =
        KanalRuntimeMeterBinder(runtimeMetrics)

    @Bean
    @ConditionalOnClass(Endpoint::class)
    @ConditionalOnProperty(prefix = "kanal", name = ["actuator-enabled"], havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    fun kanalRuntimeEndpoint(localRealtimeRuntime: LocalRealtimeRuntime): KanalRuntimeEndpoint =
        KanalRuntimeEndpoint(localRealtimeRuntime)
}
