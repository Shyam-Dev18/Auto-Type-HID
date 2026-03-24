package com.autotypehid.test.typing

import com.autotypehid.typing.mapper.KeyMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyMapperTest {

    private val mapper = KeyMapper()

    @Test
    fun lowercase_mapping() {
        val event = mapper.map('a')
        assertEquals(4.toByte(), event.keyCode)
        assertFalse(event.requiresShift)
    }

    @Test
    fun uppercase_mapping() {
        val event = mapper.map('A')
        assertEquals(4.toByte(), event.keyCode)
        assertTrue(event.requiresShift)
    }

    @Test
    fun symbols_mapping() {
        val dot = mapper.map('.')
        val comma = mapper.map(',')
        val exclamation = mapper.map('!')
        val question = mapper.map('?')

        assertEquals(55.toByte(), dot.keyCode)
        assertFalse(dot.requiresShift)

        assertEquals(54.toByte(), comma.keyCode)
        assertFalse(comma.requiresShift)

        assertEquals(30.toByte(), exclamation.keyCode)
        assertTrue(exclamation.requiresShift)

        assertEquals(56.toByte(), question.keyCode)
        assertTrue(question.requiresShift)
    }
}
