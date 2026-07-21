package flighty

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmPlatformTest {
    @Test
    fun platformNameIsDesktop() {
        assertEquals("Desktop JVM", platformName())
    }
}
