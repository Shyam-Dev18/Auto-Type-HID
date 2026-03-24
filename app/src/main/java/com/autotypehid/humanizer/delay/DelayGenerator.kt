package com.autotypehid.humanizer.delay

import com.autotypehid.humanizer.profile.BehaviorProfile
import kotlin.random.Random

class DelayGenerator {

	fun keyDelay(profile: BehaviorProfile): Long {
		return randomInRange(profile.minKeyDelayMs, profile.maxKeyDelayMs)
	}

	fun wordPause(profile: BehaviorProfile): Long {
		return randomInRange(profile.wordPauseMinMs, profile.wordPauseMaxMs)
	}

	fun sentencePause(profile: BehaviorProfile): Long {
		return randomInRange(profile.sentencePauseMinMs, profile.sentencePauseMaxMs)
	}

	private fun randomInRange(min: Long, max: Long): Long {
		if (min >= max) return min
		return Random.nextLong(from = min, until = max + 1)
	}
}
