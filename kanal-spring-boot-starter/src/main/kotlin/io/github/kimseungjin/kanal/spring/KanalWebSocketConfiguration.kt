package io.github.kimseungjin.kanal.spring

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean

@Configuration(proxyBeanMethods = false)
@EnableWebSocket
class KanalWebSocketConfiguration(
    private val properties: KanalProperties,
    private val handler: KanalWebSocketHandler,
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry
            .addHandler(handler, properties.endpoint)
            .setAllowedOriginPatterns("*")
    }

    @Bean
    fun kanalServletServerContainerFactoryBean(): ServletServerContainerFactoryBean =
        ServletServerContainerFactoryBean().apply {
            setMaxTextMessageBufferSize(properties.maxTextMessageBufferSize)
            setMaxBinaryMessageBufferSize(properties.maxBinaryMessageBufferSize)
            setMaxSessionIdleTimeout(properties.maxSessionIdleTimeout.toMillis())
            setAsyncSendTimeout(properties.asyncSendTimeout.toMillis())
        }
}
