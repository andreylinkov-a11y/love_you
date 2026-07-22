package ru.loveyou.app

import kotlin.random.Random

data class MemoryLevel(
    val number: Int,
    val columns: Int,
    val pairCount: Int,
    val previewMillis: Long,
    val maxMistakes: Int,
    val timeSeconds: Int
) {
    val cardCount: Int get() = pairCount * 2
}

data class MemoryCard(val id: Int, val symbol: String)

data class MemoryState(
    val level: MemoryLevel,
    val cards: List<MemoryCard>,
    val open: Set<Int> = emptySet(),
    val matched: Set<Int> = emptySet(),
    val mistakes: Int = 0,
    val moves: Int = 0,
    val bestCombo: Int = 0,
    val combo: Int = 0,
    val finished: Boolean = false,
    val won: Boolean = false
)

data class MemoryFlipResult(
    val state: MemoryState,
    val pairReady: Boolean = false,
    val accepted: Boolean = true
)

object MemoryEngine {
    private val symbols = listOf("🌸", "🍓", "☕", "🌙", "🐾", "🎵", "🎁", "⭐", "🦋", "🍰", "🌈", "💌", "🕯️", "🍀", "🧸")

    fun level(number: Int): MemoryLevel {
        val n = number.coerceAtLeast(1)
        val pairs = when {
            n <= 2 -> 6
            n <= 5 -> 8
            n <= 9 -> 10
            else -> 12
        }
        val columns = when (pairs) {
            6 -> 3
            8 -> 4
            10 -> 4
            else -> 4
        }
        return MemoryLevel(
            number = n,
            columns = columns,
            pairCount = pairs,
            previewMillis = (1800L - (n - 1) * 90L).coerceAtLeast(650L),
            maxMistakes = (10 - n / 2).coerceAtLeast(4),
            timeSeconds = (55 - n).coerceAtLeast(28)
        )
    }

    fun newState(levelNumber: Int, seed: Int = Random.nextInt()): MemoryState {
        val level = level(levelNumber)
        val selected = symbols.take(level.pairCount)
        val cards = selected.flatMapIndexed { index, symbol ->
            listOf(MemoryCard(index * 2, symbol), MemoryCard(index * 2 + 1, symbol))
        }.shuffled(Random(seed))
        return MemoryState(level, cards)
    }

    fun flip(state: MemoryState, index: Int): MemoryFlipResult {
        if (state.finished || index !in state.cards.indices || index in state.matched || index in state.open || state.open.size >= 2) {
            return MemoryFlipResult(state, accepted = false)
        }
        val next = state.copy(open = state.open + index)
        return MemoryFlipResult(next, pairReady = next.open.size == 2)
    }

    fun resolvePair(state: MemoryState): MemoryState {
        if (state.open.size != 2 || state.finished) return state
        val pair = state.open.toList()
        val isMatch = state.cards[pair[0]].symbol == state.cards[pair[1]].symbol
        val nextMatched = if (isMatch) state.matched + pair else state.matched
        val nextMistakes = state.mistakes + if (isMatch) 0 else 1
        val nextCombo = if (isMatch) state.combo + 1 else 0
        val won = nextMatched.size == state.cards.size
        val lost = !won && nextMistakes >= state.level.maxMistakes
        return state.copy(
            open = emptySet(),
            matched = nextMatched,
            mistakes = nextMistakes,
            moves = state.moves + 1,
            combo = nextCombo,
            bestCombo = maxOf(state.bestCombo, nextCombo),
            finished = won || lost,
            won = won
        )
    }

    fun timeout(state: MemoryState): MemoryState = if (state.won) state else state.copy(finished = true, won = false, open = emptySet())
}
