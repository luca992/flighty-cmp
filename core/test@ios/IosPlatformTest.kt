package flighty

import kotlin.test.Test
import kotlin.test.assertEquals

class IosPlatformTest {
    @Test
    fun platformNameIsIos() {
        assertEquals("iOS", platformName())
    }
}
