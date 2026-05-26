package io.github.kimseungjin.kanal.spring

import io.github.kimseungjin.kanal.core.PresenceStore
import io.github.kimseungjin.kanal.core.RealtimeApplication
import io.github.kimseungjin.kanal.runtime.LocalRealtimeRuntime
import io.github.kimseungjin.kanal.runtime.RuntimeMetrics
import io.micrometer.core.instrument.binder.MeterBinder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertNotNull

@SpringBootTest(classes = [KanalAutoConfiguration::class])
class KanalAutoConfigurationTest(
    @Autowired private val presenceStore: PresenceStore,
    @Autowired private val realtimeApplication: RealtimeApplication,
    @Autowired private val runtimeMetrics: RuntimeMetrics,
    @Autowired private val localRealtimeRuntime: LocalRealtimeRuntime,
    @Autowired private val kanalWebSocketHandler: KanalWebSocketHandler,
    @Autowired private val kanalWebSocketConfiguration: KanalWebSocketConfiguration,
    @Autowired private val kanalRuntimeMeterBinder: MeterBinder,
    @Autowired private val kanalRuntimeEndpoint: KanalRuntimeEndpoint,
) {
    @Test
    fun `registers default beans`() {
        assertNotNull(presenceStore)
        assertNotNull(realtimeApplication)
        assertNotNull(runtimeMetrics)
        assertNotNull(localRealtimeRuntime)
        assertNotNull(kanalWebSocketHandler)
        assertNotNull(kanalWebSocketConfiguration)
        assertNotNull(kanalRuntimeMeterBinder)
        assertNotNull(kanalRuntimeEndpoint)
    }
}
