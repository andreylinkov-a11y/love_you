package ru.loveyou.app

import org.junit.Assert.*
import org.junit.Test

class MemoryEngineTest {
    @Test fun boardContainsExactPairs() {
        repeat(20) { seed ->
            val state = MemoryEngine.newState(8, seed)
            assertEquals(state.level.cardCount, state.cards.size)
            state.cards.groupBy { it.symbol }.values.forEach { assertEquals(2, it.size) }
        }
    }

    @Test fun levelDifficultyGrowsSafely() {
        val first = MemoryEngine.level(1)
        val later = MemoryEngine.level(12)
        assertTrue(later.pairCount > first.pairCount)
        assertTrue(later.previewMillis < first.previewMillis)
        assertTrue(later.maxMistakes <= first.maxMistakes)
        assertTrue(later.timeSeconds < first.timeSeconds)
    }

    @Test fun matchingPairBecomesPermanent() {
        val state = MemoryEngine.newState(1, 42)
        val grouped = state.cards.withIndex().groupBy { it.value.symbol }
        val pair = grouped.values.first().map { it.index }
        var next = MemoryEngine.flip(state, pair[0]).state
        next = MemoryEngine.flip(next, pair[1]).state
        next = MemoryEngine.resolvePair(next)
        assertTrue(pair.all { it in next.matched })
        assertEquals(1, next.combo)
        assertEquals(0, next.mistakes)
    }

    @Test fun wrongPairCostsMistakeAndResetsCombo() {
        val state = MemoryEngine.newState(1, 7)
        val first = 0
        val second = state.cards.indices.first { state.cards[it].symbol != state.cards[first].symbol }
        var next = state.copy(combo = 3)
        next = MemoryEngine.flip(next, first).state
        next = MemoryEngine.flip(next, second).state
        next = MemoryEngine.resolvePair(next)
        assertEquals(1, next.mistakes)
        assertEquals(0, next.combo)
        assertTrue(next.open.isEmpty())
    }

    @Test fun timeoutFinishesWithoutWin() {
        val state = MemoryEngine.timeout(MemoryEngine.newState(2, 1))
        assertTrue(state.finished)
        assertFalse(state.won)
    }
}
