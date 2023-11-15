package org.openmbee.flexo.mms

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.openmbee.flexo.mms.auth.shouldEscape
import org.openmbee.flexo.mms.auth.ESCAPE_CHARACTERS

class CharExtensionsTest {
    @Test
    fun testShouldEscape() {
        ESCAPE_CHARACTERS.forEach { char ->
            assertTrue(char.shouldEscape() as Boolean)
        }
    }
}