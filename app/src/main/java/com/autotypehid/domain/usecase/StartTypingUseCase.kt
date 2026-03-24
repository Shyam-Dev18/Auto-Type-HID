package com.autotypehid.domain.usecase

import com.autotypehid.humanizer.delay.DelayGenerator
import com.autotypehid.humanizer.profile.BehaviorProfile
import com.autotypehid.humanizer.typo.TypoEngine
import com.autotypehid.typing.mapper.KeyEvent
import com.autotypehid.typing.mapper.KeyMapper
import com.autotypehid.typing.parser.TextParser
import com.autotypehid.typing.queue.TypingQueue

enum class TypingActionType {
    KEY,
    DELAY
}

data class TypingAction(
    val type: TypingActionType,
    val keyEvent: KeyEvent? = null,
    val delayMs: Long? = null
)

class StartTypingUseCase(
    private val textParser: TextParser = TextParser(),
    private val keyMapper: KeyMapper = KeyMapper(),
    private val typingQueue: TypingQueue = TypingQueue(),
    private val delayGenerator: DelayGenerator = DelayGenerator(),
    private val typoEngine: TypoEngine = TypoEngine()
) {

    fun prepare(text: String): List<KeyEvent> {
        val chars = textParser.parse(text)
        val keyEvents = chars.map { keyMapper.map(it) }
        typingQueue.enqueue(keyEvents)
        return keyEvents
    }

    fun prepare(text: String, profile: BehaviorProfile): List<TypingAction> {
        typingQueue.clear()
        val characters = textParser.parse(text)
        if (characters.isEmpty()) return emptyList()

        val actions = mutableListOf<TypingAction>()

        characters.forEach { character ->
            if (typoEngine.shouldMakeTypo(profile)) {
                val typoChars = typoEngine.correctionSequence(character)
                typoChars.forEach { typoChar ->
                    addKeyAndDelay(actions, keyMapper.map(typoChar), profile)
                }
            } else {
                addKeyAndDelay(actions, keyMapper.map(character), profile)
            }

            when (character) {
                ' ' -> actions.add(
                    TypingAction(
                        type = TypingActionType.DELAY,
                        delayMs = delayGenerator.wordPause(profile)
                    )
                )
                '.', '!', '?' -> actions.add(
                    TypingAction(
                        type = TypingActionType.DELAY,
                        delayMs = delayGenerator.sentencePause(profile)
                    )
                )
            }
        }

        return actions
    }

    private fun addKeyAndDelay(
        actions: MutableList<TypingAction>,
        keyEvent: KeyEvent,
        profile: BehaviorProfile
    ) {
        typingQueue.enqueue(listOf(keyEvent))
        actions.add(
            TypingAction(
                type = TypingActionType.KEY,
                keyEvent = keyEvent
            )
        )
        actions.add(
            TypingAction(
                type = TypingActionType.DELAY,
                delayMs = delayGenerator.keyDelay(profile)
            )
        )
    }
}
