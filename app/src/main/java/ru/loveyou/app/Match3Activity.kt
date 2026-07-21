package ru.loveyou.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlinx.coroutines.launch

private val MatchPlum = Color(0xFF4D1838)
private val MatchBerry = Color(0xFF8E2D56)
private val MatchRose = Color(0xFFF05283)
private val MatchBlush = Color(0xFFFFE7F0)
private val MatchCream = Color(0xFFFFF8FB)
private val MatchGold = Color(0xFFFFD36E)

class Match3Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Match3App(onExit = { finish() }) }
    }
}

@Composable
private fun Match3App(onExit: () -> Unit) {
    val context = LocalContext.current
    val store = remember { ProgressStore(context) }
    val progress by store.progress.collectAsState(initial = GameProgress())
    val scope = rememberCoroutineScope()
    var state by remember(progress.match3Level) { mutableStateOf(Match3Engine.newGame(progress.match3Level)) }
    var reward by remember { mutableStateOf<Match3RunResult?>(null) }

    MaterialTheme(colorScheme = lightColorScheme(primary = MatchRose, background = MatchCream)) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(MatchPlum, MatchBerry, MatchRose, MatchCream))
            )
        ) {
            Match3Screen(
                state = state,
                onExit = onExit,
                onSwap = { first, second ->
                    val move = Match3Engine.playSwap(state, first, second)
                    state = move.state
                },
                onRestart = { state = Match3Engine.newGame(progress.match3Level, seed = System.nanoTime().toInt()) },
                onClaim = {
                    scope.launch {
                        reward = store.completeMatch3Level(state.score, state.movesLeft, state.bestCascade)
                    }
                }
            )
            reward?.let { result ->
                Match3Reward(result) {
                    reward = null
                    state = Match3Engine.newGame(result.level + 1)
                }
            }
        }
    }
}

@Composable
private fun Match3Screen(
    state: Match3State,
    onExit: () -> Unit,
    onSwap: (Int, Int) -> Unit,
    onRestart: () -> Unit,
    onClaim: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onExit) { Text("Назад") }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Три в ряд · уровень ${state.level.number}", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("Свайпни фишку к соседней", color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .97f)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🎯 ${state.score} / ${state.level.targetScore}", color = MatchPlum, fontWeight = FontWeight.Black)
                    Text("Ходы: ${state.movesLeft}", color = MatchRose, fontWeight = FontWeight.Black)
                    Text("Каскад: ×${state.bestCascade}", color = MatchPlum, fontWeight = FontWeight.Black)
                }
                LinearProgressIndicator(
                    progress = { (state.score.toFloat() / state.level.targetScore).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = MatchRose,
                    trackColor = MatchBlush
                )
                state.level.targetGem?.let { target ->
                    Text("Собери ${target.emoji}: ${state.collected} / ${state.level.targetCount}", color = MatchBerry, fontWeight = FontWeight.Bold)
                }
            }
        }

        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val boardSize = minOf(maxWidth, maxHeight, 520.dp)
            Column(
                Modifier.size(boardSize).clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha = .22f)).padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (row in 0 until state.level.size) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (column in 0 until state.level.size) {
                            val index = row * state.level.size + column
                            Match3Tile(
                                gem = state.board[index],
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                                onSwipe = { horizontal, positive ->
                                    val next = when {
                                        horizontal && positive && column < state.level.size - 1 -> index + 1
                                        horizontal && !positive && column > 0 -> index - 1
                                        !horizontal && positive && row < state.level.size - 1 -> index + state.level.size
                                        !horizontal && !positive && row > 0 -> index - state.level.size
                                        else -> index
                                    }
                                    if (next != index) onSwap(index, next)
                                }
                            )
                        }
                    }
                }
            }
        }

        Text(state.lastMessage, color = MatchPlum, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        when {
            state.won -> Button(onClick = onClaim, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                Text("Забрать награду и открыть уровень ${state.level.number + 1}", fontWeight = FontWeight.ExtraBold)
            }
            state.lost -> Button(onClick = onRestart, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                Text("Попробовать ещё раз", fontWeight = FontWeight.ExtraBold)
            }
            else -> Text("Каждый каскад умножает очки. Планируй ход, а не торопись.", color = Color(0xFF6E4A5A), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun Match3Tile(gem: Gem, modifier: Modifier, onSwipe: (horizontal: Boolean, positive: Boolean) -> Unit) {
    var dragX by remember(gem) { mutableFloatStateOf(0f) }
    var dragY by remember(gem) { mutableFloatStateOf(0f) }
    var pressed by remember(gem) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.10f else 1f,
        animationSpec = spring(dampingRatio = .55f, stiffness = 520f),
        label = "gem-scale"
    )
    val color = when (gem) {
        Gem.HEART -> Color(0xFFFFD9E5)
        Gem.FLOWER -> Color(0xFFFFE9C7)
        Gem.STAR -> Color(0xFFFFF3B0)
        Gem.GIFT -> Color(0xFFDCCBFF)
        Gem.BERRY -> Color(0xFFFFD2D2)
        Gem.MOON -> Color(0xFFD7E6FF)
    }
    Box(
        modifier.scale(scale).clip(RoundedCornerShape(14.dp)).background(color).pointerInput(gem) {
            detectDragGestures(
                onDragStart = { pressed = true; dragX = 0f; dragY = 0f },
                onDragCancel = { pressed = false; dragX = 0f; dragY = 0f },
                onDragEnd = {
                    pressed = false
                    val horizontal = abs(dragX) >= abs(dragY)
                    val magnitude = if (horizontal) abs(dragX) else abs(dragY)
                    if (magnitude > 18f) onSwipe(horizontal, if (horizontal) dragX > 0 else dragY > 0)
                    dragX = 0f
                    dragY = 0f
                }
            ) { change, amount ->
                change.consume()
                dragX += amount.x
                dragY += amount.y
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Text(gem.emoji, fontSize = 29.sp)
    }
}

@Composable
private fun Match3Reward(result: Match3RunResult, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .5f)).padding(22.dp), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(18.dp)) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🎊", fontSize = 62.sp)
                Text("Уровень ${result.level} пройден!", color = MatchPlum, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("⭐".repeat(result.stars), fontSize = 30.sp)
                Text("+${result.reward} любви", color = MatchRose, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("${result.score} очков · осталось ходов ${result.movesLeft} · каскад ×${result.bestCascade}", textAlign = TextAlign.Center)
                if ((result.level + 1) % 5 == 0) {
                    Text("🎁 Открыт большой сюрприз за пять уровней!", color = MatchBerry, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Следующий уровень") }
            }
        }
    }
}
