package com.autotypehid.humanizer.typo

import com.autotypehid.humanizer.profile.BehaviorProfile
import kotlin.random.Random

class TypoEngine {

	private val typoPool: List<Char> = ('a'..'z').toList()

	fun shouldMakeTypo(profile: BehaviorProfile): Boolean {
		val probability = profile.typoProbability.coerceIn(0f, 1f)
		return Random.nextFloat() < probability
	}

	fun generateTypo(char: Char): Char {
		if (typoPool.isEmpty()) return 'x'
		val normalized = char.lowercaseChar()
		val candidates = typoPool.filter { it != normalized }
		if (candidates.isEmpty()) return 'x'
		return candidates.random()
	}

	fun correctionSequence(original: Char): List<Char> {
		val typo = generateTypo(original)
		return listOf(typo, '\b', original)
	}
}
