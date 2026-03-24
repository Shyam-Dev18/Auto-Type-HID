package com.autotypehid.test.humanizer

import com.autotypehid.humanizer.delay.DelayGenerator
import com.autotypehid.humanizer.profile.DefaultProfiles
import org.junit.Assert.assertTrue
import org.junit.Test

class DelayGeneratorTest {

    private val generator = DelayGenerator()

    @Test
    fun delay_range_correctness() {
        val profile = DefaultProfiles.NORMAL

        repeat(100) {
            val key = generator.keyDelay(profile)
            val word = generator.wordPause(profile)
            val sentence = generator.sentencePause(profile)

            assertTrue(key in profile.minKeyDelayMs..profile.maxKeyDelayMs)
            assertTrue(word in profile.wordPauseMinMs..profile.wordPauseMaxMs)
            assertTrue(sentence in profile.sentencePauseMinMs..profile.sentencePauseMaxMs)
        }
    }
}
