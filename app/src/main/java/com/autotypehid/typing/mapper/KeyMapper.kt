package com.autotypehid.typing.mapper

data class KeyEvent(
    val keyCode: Byte,
    val requiresShift: Boolean
)

class KeyMapper {

    companion object {
        private const val HID_A: Int = 4
        private const val HID_1: Byte = 30
        private const val HID_SPACE: Byte = 44
        private const val HID_COMMA: Byte = 54
        private const val HID_DOT: Byte = 55
        private const val HID_SLASH: Byte = 56
        private const val HID_BACKSPACE: Byte = 42
    }

    fun map(char: Char): KeyEvent {
        return when {
            char in 'a'..'z' -> KeyEvent(
                keyCode = (HID_A + (char - 'a')).toByte(),
                requiresShift = false
            )
            char in 'A'..'Z' -> KeyEvent(
                keyCode = (HID_A + (char - 'A')).toByte(),
                requiresShift = true
            )
            char == ' ' -> KeyEvent(
                keyCode = HID_SPACE,
                requiresShift = false
            )
            char == '.' -> KeyEvent(
                keyCode = HID_DOT,
                requiresShift = false
            )
            char == ',' -> KeyEvent(
                keyCode = HID_COMMA,
                requiresShift = false
            )
            char == '!' -> KeyEvent(
                keyCode = HID_1,
                requiresShift = true
            )
            char == '?' -> KeyEvent(
                keyCode = HID_SLASH,
                requiresShift = true
            )
            char == '\b' -> KeyEvent(
                keyCode = HID_BACKSPACE,
                requiresShift = false
            )
            else -> KeyEvent(
                keyCode = HID_SPACE,
                requiresShift = false
            )
        }
    }
}
