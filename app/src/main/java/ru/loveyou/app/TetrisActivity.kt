package ru.loveyou.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private val TPlum = Color(0xFF4D1838)
private val TBerry = Color(0xFF8E2D56)
private val TRose = Color(0xFFF05283)
private val TBlush = Color(0xFFFFE7F0)
private val TGold = Color(0xFFFFD36E)
private val TMint = Color(0xFF9FE3CC)

class TetrisActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FallingHeartsGame(onExit = { finish() }) }
    }
}

@Composable
private fun FallingHeartsGame(onExit: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val store = remember { ProgressStore(context) }
    val prefs = remember { context.getSharedPreferences("falling_hearts_progress", 0) }
    var state by remember { mutableStateOf(TetrisEngine.newState()) }
    var paused by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var rewarded by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Свайп — движение · тап — поворот") }

    LaunchedEffect(state.level, state.gameOver, paused) {
        while (!state.gameOver && !paused) {
            delay(state.dropDelayMs)
            state = TetrisEngine.softDrop(state)
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = TRose, background = TBlush)) {
        Column(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(TPlum, TBerry, TRose, TBlush)))
                .safeDrawingPadding().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onExit) { Text("Назад", color = Color.White) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Падающие сердца", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("Уровень ${state.level} · скорость растёт каждые 8 линий", color = Color.White.copy(alpha = .8f), fontSize = 11.sp)
                }
                TextButton(onClick = { paused = !paused }) { Text(if (paused) "▶" else "Ⅱ", color = Color.White, fontSize = 20.sp) }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TStat("Очки", state.score)
                TStat("Линии", state.lines)
                TStat("Комбо", state.combo)
                TStat("Рекорд", maxOf(state.score, prefs.getInt("best", 0)))
            }
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF251421)),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                BoxWithConstraints(
                    Modifier.fillMaxSize().padding(8.dp)
                        .pointerInput(state.gameOver, paused) {
                            detectTapGestures {
                                if (!state.gameOver && !paused) {
                                    state = TetrisEngine.rotate(state)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                        .pointerInput(state.gameOver, paused) {
                            detectDragGestures(
                                onDragStart = { dragX = 0f; dragY = 0f },
                                onDrag = { change, amount ->
                                    change.consume()
                                    if (state.gameOver || paused) return@detectDragGestures
                                    dragX += amount.x
                                    dragY += amount.y
                                    val horizontalThreshold = size.width / 11f
                                    val verticalThreshold = size.height / 22f
                                    if (abs(dragX) >= horizontalThreshold) {
                                        state = TetrisEngine.move(state, if (dragX > 0) 1 else -1)
                                        dragX = 0f
                                    }
                                    if (dragY >= verticalThreshold) {
                                        state = TetrisEngine.softDrop(state)
                                        dragY = 0f
                                    }
                                },
                                onDragEnd = {
                                    if (!state.gameOver && !paused && dragY > size.height * .10f) {
                                        state = TetrisEngine.hardDrop(state)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        message = "Мгновенный сброс! ✨"
                                    }
                                    dragX = 0f; dragY = 0f
                                }
                            )
                        }
                ) {
                    val boardRatio = state.columns.toFloat() / state.rows
                    Canvas(Modifier.fillMaxSize().aspectRatio(boardRatio, matchHeightConstraintsFirst = true).align(Alignment.Center)) {
                        val cell = minOf(size.width / state.columns, size.height / state.rows)
                        val boardWidth = cell * state.columns
                        val boardHeight = cell * state.rows
                        val left = (size.width - boardWidth) / 2f
                        val top = (size.height - boardHeight) / 2f
                        for (r in 0 until state.rows) for (c in 0 until state.columns) {
                            drawRoundRect(Color.White.copy(alpha = .035f), Offset(left + c * cell + 1f, top + r * cell + 1f), Size(cell - 2f, cell - 2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * .18f))
                        }
                        val ghost = TetrisEngine.ghostCells(state).toSet()
                        ghost.forEach { drawBlock(it, left, top, cell, TRose.copy(alpha = .18f), true) }
                        state.board.forEach { drawBlock(it, left, top, cell, TMint, false) }
                        state.active.cells().filter { it.row >= 0 }.forEach { drawBlock(it, left, top, cell, TRose, false) }
                        drawRoundRect(Color.White.copy(alpha = .22f), Offset(left, top), Size(boardWidth, boardHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * .3f), style = Stroke(2f))
                    }
                    if (paused && !state.gameOver) {
                        Surface(Modifier.align(Alignment.Center), shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = .78f)) {
                            Text("Пауза\nКоснись Ⅱ, чтобы продолжить", color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(22.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(message, color = TPlum, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))

            if (!state.gameOver) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { state = TetrisEngine.move(state, -1) }, modifier = Modifier.weight(1f)) { Text("←") }
                    Button(onClick = { state = TetrisEngine.rotate(state) }, modifier = Modifier.weight(1f)) { Text("↻") }
                    OutlinedButton(onClick = { state = TetrisEngine.move(state, 1) }, modifier = Modifier.weight(1f)) { Text("→") }
                    Button(onClick = { state = TetrisEngine.hardDrop(state) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = TGold)) { Text("↓", color = TPlum) }
                }
            } else {
                val reward = (20 + state.level * 8 + state.lines * 3 + state.bestCombo * 5).coerceAtMost(300)
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .96f))) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Приключение завершено", color = TPlum, fontWeight = FontWeight.Black, fontSize = 19.sp)
                        Text("${state.score} очков · ${state.lines} линий · +$reward любви", color = TRose, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                prefs.edit().putInt("best", maxOf(prefs.getInt("best", 0), state.score)).apply()
                                if (!rewarded) scope.launch { store.completeGame(reward); rewarded = true }
                                state = TetrisEngine.newState(); rewarded = false; paused = false
                            }, modifier = Modifier.weight(1f)) { Text("Ещё раз") }
                            OutlinedButton(onClick = {
                                prefs.edit().putInt("best", maxOf(prefs.getInt("best", 0), state.score)).apply()
                                if (!rewarded) scope.launch { store.completeGame(reward); rewarded = true }
                                onExit()
                            }, modifier = Modifier.weight(1f)) { Text("В приключения") }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlock(cell: Cell, left: Float, top: Float, size: Float, color: Color, outline: Boolean) {
    val offset = Offset(left + cell.column * size + 2f, top + cell.row * size + 2f)
    val blockSize = Size(size - 4f, size - 4f)
    drawRoundRect(color, offset, blockSize, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * .22f), style = if (outline) Stroke(2.5f) else androidx.compose.ui.graphics.drawscope.Fill)
    if (!outline) drawCircle(Color.White.copy(alpha = .34f), radius = size * .10f, center = Offset(offset.x + size * .32f, offset.y + size * .30f))
}

@Composable
private fun TStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
        Text(label, color = Color.White.copy(alpha = .72f), fontSize = 10.sp)
    }
}