package com.autotypehid.typing.queue

import com.autotypehid.typing.mapper.KeyEvent
import java.util.ArrayDeque

class TypingQueue {
    
    private val queue: ArrayDeque<KeyEvent> = ArrayDeque()
    
    fun enqueue(events: List<KeyEvent>) {
        queue.addAll(events)
    }
    
    fun next(): KeyEvent? {
        return if (queue.isNotEmpty()) queue.removeFirst() else null
    }
    
    fun clear() {
        queue.clear()
    }
    
    fun size(): Int = queue.size
}
