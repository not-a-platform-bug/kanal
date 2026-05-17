package io.github.kimseungjin.kanal.spring

import io.github.kimseungjin.kanal.core.InMemoryPresenceStore
import io.github.kimseungjin.kanal.core.PresenceStore
import io.github.kimseungjin.kanal.core.RealtimeApplication
import io.github.kimseungjin.kanal.core.dsl.realtime
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(KanalProperties::class)
class KanalAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun kanalPresenceStore(): PresenceStore = InMemoryPresenceStore()

    @Bean
    @ConditionalOnMissingBean
    fun kanalRealtimeApplication(): RealtimeApplication = realtime {}
}
