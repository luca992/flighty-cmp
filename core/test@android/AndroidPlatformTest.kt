package flighty

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidPlatformTest {
    @Test
    fun platformNameIsAndroid() {
        assertEquals("Android", platformName())
    }
}
