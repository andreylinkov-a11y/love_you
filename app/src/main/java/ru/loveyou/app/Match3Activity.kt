package ru.loveyou.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.loveyou.app.match3.Cell
import ru.loveyou.app.match3.Gem
import ru.loveyou.app.match3.Match3Engine
import ru.loveyou.app.match3.Match3State
import kotlin.math.abs

private val MatchPlum = Color(0xFF4D1838)
private val MatchRose = Color(0xFFF05283)
private val MatchBlush = Color(0xFFFFE7F0)
private val MatchCream = Color(0xFFFFF8FB)
private val MatchGold = Color(0xFFFFD36E)

class Match3Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Match3App(onClose = { finish() }) }
    }
}

@Composable
private fun Match3App(onClose: () -> Unit) {
    val engine = remember { Match3Engine() }
    var state by remember { mutableStateOf(engine.newGame(1)) }
    var message by remember { mutableStateOf("Проведи фишку к соседней — собери три или больше") }
    var lastMoved by remember { mutableStateOf<Set<Int>>(emptySet()) }

    MaterialTheme(colorScheme = lightColorScheme(primary = MatchRose, background = MatchCream)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(MatchPlum, MatchRose, MatchCream)))
                .safeDrawingPadding()
                .padding(12.dp)
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onClose) { Text("Назад", color = Color.White) }
                    Spacer(Modifier.weight(1f))
                    Text("Три в ряд · уровень ${state.level.number}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .97f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ходы: ${state.movesLeft}", color = MatchPlum, fontWeight = FontWeight.ExtraBold)
                            Text("Счёт: ${state.score}", color = MatchRose, fontWeight = FontWeight.ExtraBold)
                            Text("Комбо: ×${state.combo}", color = MatchPlum, fontWeight = FontWeight.ExtraBold)
                        }
                        Text("Собери ${emoji(state.level.targetGem)}: ${state.collected} / ${state.level.targetCount}", color = MatchPlum)
                        LinearProgressIndicator(
                            progress = { (state.collected.toFloat() / state.level.targetCount).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            color = MatchRose,
                            trackColor = MatchBlush
                        )
                    }
                }

                MatchBoard(
                    state = state,
                    highlighted = lastMoved,
                    onSwap = { first, second ->
                        val turn = engine.swap(state, first, second)
                        if (turn.accepted) {
                            state = turn.state
                            lastMoved = setOf(state.index(first), state.index(second))
                            message = when {
                                turn.cascades >= 3 -> "Великолепный каскад ×${turn.cascades}! ✨"
                                turn.cascades == 2 -> "Двойной каскад!"
                                else -> "Собрано ${turn.removed} фишек"
                            }
                        } else {
                            lastMoved = emptySet()
                            message = "Этот ход не создаёт комбинацию"
                        }
                    }
                )

                Text(message, color = MatchPlum, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

                if (state.finished) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (state.won) MatchGold else Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(if (state.won) "Уровень пройден! 🎉" else "Ходы закончились", color = MatchPlum, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Button(onClick = {
                                val next = if (state.won) state.level.number + 1 else state.level.number
                                state = engine.newGame(next)
                                lastMoved = emptySet()
                                message = if (state.won) "Новый уровень сложнее" else "Попробуй другую стратегию"
                            }) {
                                Text(if (state.won) "Следующий уровень" else "Повторить")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchBoard(
    state: Match3State,
    highlighted: Set<Int>,
    onSwap: (Cell, Cell) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(state.level.cols),
        modifier = Modifier.fillMaxWidth().aspectRatio(state.level.cols.toFloat() / state.level.rows),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        userScrollEnabled = false
    ) {
        items(state.board.indices.toList(), key = { it }) { index ->
            val row = index / state.level.cols
            val col = index % state.level.cols
            val active = index in highlighted
            val scale by animateFloatAsState(if (active) 1.10f else 1f, label = "gem-scale-$index")
            var dx by remember { mutableStateOf(0f) }
            var dy by remember { mutableStateOf(0f) }

            Box(
                Modifier
                    .aspectRatio(1f)
                    .scale(scale)
                    .clip(RoundedCornerShape(14.dp))
                    .background(gemColor(state.board[index]))
                    .pointerInput(state.board, state.finished) {
                        detectDragGestures(
                            onDragStart = { dx = 0f; dy = 0f },
                            onDrag = { change, drag ->
                                change.consume()
                                dx += drag.x
                                dy += drag.y
                            },
                            onDragEnd = {
                                if (!state.finished) {
                                    val threshold = size.minDimension * .24f
                                    val target = when {
                                        abs(dx) > abs(dy) && dx > threshold -> Cell(row, col + 1)
                                        abs(dx) > abs(dy) && dx < -threshold -> Cell(row, col - 1)
                                        dy > threshold -> Cell(row + 1, col)
                                        dy < -threshold -> Cell(row - 1, col)
                                        else -> null
                                    }
                                    if (target != null) onSwap(Cell(row, col), target)
                                }
                                dx = 0f; dy = 0f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji(state.board[index]), fontSize = 28.sp)
            }
        }
    }
}

private fun emoji(gem: Gem): String = when (gem) {
    Gem.HEART -> "❤️"
    Gem.STAR -> "⭐"
    Gem.FLOWER -> "🌸"
    Gem.GIFT -> "🎁"
    Gem.MOON -> "🌙"
    Gem.BERRY -> "🍓"
}

private fun gemColor(gem: Gem): Color = when (gem) {
    Gem.HEART -> Color(0xFFFFCADC)
    Gem.STAR -> Color(0xFFFFEDB3)
    Gem.FLOWER -> Color(0xFFE5D1FF)
    Gem.GIFT -> Color(0xFFCDE9FF)
    Gem.MOON -> Color(0xFFD8D9FF)
    Gem.BERRY -> Color(0xFFFFD6D0)
}
