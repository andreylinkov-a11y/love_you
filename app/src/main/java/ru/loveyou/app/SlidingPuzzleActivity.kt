package ru.loveyou.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val PuzzlePlum = Color(0xFF4D1838)
private val PuzzleBerry = Color(0xFF8E2D56)
private val PuzzleRose = Color(0xFFF05283)
private val PuzzleCream = Color(0xFFFFF8FB)
private val PuzzleGold = Color(0xFFFFD36E)

class SlidingPuzzleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SlidingPuzzleAdventure { finish() } }
    }
}

@Composable
private fun SlidingPuzzleAdventure(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sliding_puzzle_progress", 0) }
    val store = remember { ProgressStore(context) }
    val haptic = LocalHapticFeedback.current
    var levelNumber by remember { mutableIntStateOf(prefs.getInt("level", 1).coerceAtLeast(1)) }
    var state by remember(levelNumber) { mutableStateOf(SlidingPuzzleEngine.newState(levelNumber)) }
    var message by remember { mutableStateOf("Проводи плитки к пустому месту") }
    var rewarded by remember(levelNumber) { mutableStateOf(false) }

    MaterialTheme(colorScheme = lightColorScheme(primary = PuzzleRose, background = PuzzleCream)) {
        Column(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PuzzlePlum, PuzzleBerry, PuzzleRose, PuzzleCream))).safeDrawingPadding().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onClose) { Text("Назад", color = Color.White) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Собери послание · уровень $levelNumber", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Сдвигай пальцем сразу целую линию", color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .97f))) {
                Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ходы ${state.moves}", color = PuzzlePlum, fontWeight = FontWeight.Black)
                    Text("Ориентир ${state.level.parMoves}", color = PuzzleRose, fontWeight = FontWeight.Black)
                    Text("Дистанция ${SlidingPuzzleEngine.manhattanDistance(state.board, state.level.size)}", color = PuzzlePlum, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val boardSize = minOf(maxWidth, maxHeight, 430.dp)
                Column(
                    Modifier.size(boardSize).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .96f)).padding(7.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(state.level.size) { row ->
                        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(state.level.size) { col ->
                                val index = row * state.level.size + col
                                val value = state.board[index]
                                PuzzleTile(
                                    value = value,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    enabled = !state.finished,
                                    onSwipe = {
                                        val result = SlidingPuzzleEngine.move(state, index)
                                        if (result.accepted) {
                                            state = result.state
                                            message = if (result.shiftedTiles > 1) "Сдвинута целая линия — красиво!" else "Точный ход"
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else {
                                            message = "Эта плитка не связана с пустым местом"
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Text(message, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            if (state.finished) {
                val efficiency = (state.level.parMoves.toFloat() / state.moves.coerceAtLeast(1)).coerceAtMost(1.4f)
                val reward = (35 + levelNumber * 8 + efficiency * 24).toInt()
                LaunchedEffect(levelNumber, rewarded) {
                    if (!rewarded) { rewarded = true; store.completeGame(reward) }
                }
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = PuzzleGold)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Послание собрано! 💖", color = PuzzlePlum, fontWeight = FontWeight.Black, fontSize = 21.sp)
                        Text("+$reward любви · ${state.moves} ходов", color = PuzzlePlum)
                        Button(onClick = { levelNumber++; prefs.edit().putInt("level", levelNumber).apply() }, modifier = Modifier.fillMaxWidth()) { Text("Следующий уровень") }
                    }
                }
            } else {
                TextButton(onClick = { state = SlidingPuzzleEngine.newState(levelNumber); message = "Новое решаемое поле готово" }) { Text("Перемешать заново", color = Color.White) }
            }
        }
    }
}

@Composable
private fun PuzzleTile(value: Int, modifier: Modifier, enabled: Boolean, onSwipe: () -> Unit) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (dragging) 1.07f else 1f, spring(dampingRatio = .6f, stiffness = 520f), label = "tile")
    Box(
        modifier.scale(scale).clip(RoundedCornerShape(15.dp)).background(if (value == 0) Color.Transparent else PuzzleRose)
            .pointerInput(enabled, value) {
                if (enabled && value != 0) detectDragGestures(
                    onDragStart = { dragging = true; dragX = 0f; dragY = 0f },
                    onDrag = { change, amount -> change.consume(); dragX += amount.x; dragY += amount.y },
                    onDragEnd = { dragging = false; if (abs(dragX) + abs(dragY) > 16f) onSwipe(); dragX = 0f; dragY = 0f },
                    onDragCancel = { dragging = false; dragX = 0f; dragY = 0f }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) Text("$value", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
    }
}
