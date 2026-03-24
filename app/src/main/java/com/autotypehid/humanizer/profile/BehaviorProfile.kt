package com.autotypehid.humanizer.profile

data class BehaviorProfile(
	val minKeyDelayMs: Long,
	val maxKeyDelayMs: Long,
	val wordPauseMinMs: Long,
	val wordPauseMaxMs: Long,
	val sentencePauseMinMs: Long,
	val sentencePauseMaxMs: Long,
	val typoProbability: Float
)

object DefaultProfiles {
	val NORMAL = BehaviorProfile(
		minKeyDelayMs = 45L,
		maxKeyDelayMs = 120L,
		wordPauseMinMs = 120L,
		wordPauseMaxMs = 280L,
		sentencePauseMinMs = 320L,
		sentencePauseMaxMs = 700L,
		typoProbability = 0.08f
	)

	val FAST = BehaviorProfile(
		minKeyDelayMs = 20L,
		maxKeyDelayMs = 70L,
		wordPauseMinMs = 70L,
		wordPauseMaxMs = 160L,
		sentencePauseMinMs = 180L,
		sentencePauseMaxMs = 360L,
		typoProbability = 0.04f
	)

	val SLOW = BehaviorProfile(
		minKeyDelayMs = 90L,
		maxKeyDelayMs = 220L,
		wordPauseMinMs = 220L,
		wordPauseMaxMs = 420L,
		sentencePauseMinMs = 600L,
		sentencePauseMaxMs = 1200L,
		typoProbability = 0.12f
	)
}
