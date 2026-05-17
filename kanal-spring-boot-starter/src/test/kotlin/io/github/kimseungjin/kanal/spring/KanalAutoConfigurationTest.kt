package io.github.kimseungjin.kanal.spring

import io.github.kimseungjin.kanal.core.PresenceStore
import io.github.kimseungjin.kanal.core.RealtimeApplication
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertNotNull

@SpringBootTest(classes = [KanalAutoConfiguration::class])
class KanalAutoConfigurationTest(
    @Autowired private val presenceStore: PresenceStore,
    @Autowired private val realtimeApplication: RealtimeApplication,
) {
    @Test
    fun `registers default beans`() {
        assertNotNull(presenceStore)
        assertNotNull(realtimeApplication)
    }
}
