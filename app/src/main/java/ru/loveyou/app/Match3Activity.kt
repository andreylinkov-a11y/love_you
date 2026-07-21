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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import kotlin.random.Random

private val MatchPlum = Color(0xFF4D1838)
private val MatchBerry = Color(0xFF8E2D56)
private val MatchRose = Color(0xFFF05283)
private val MatchBlush = Color(0xFFFFE7F0)
private val MatchCream = Color(0xFFFFF8FB)
private val MatchGold = Color(0xFFFFD36E)

class Match3Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Match3Adventure(onClose = { finish() }) }
    }
}

@Composable
private fun Match3Adventure(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("match3_progress", 0) }
    val progressStore = remember { ProgressStore(context) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var levelNumber by remember { mutableIntStateOf(prefs.getInt("level", 1).coerceAtLeast(1)) }
    var state by remember(levelNumber) { mutableStateOf(Match3Engine.newState(levelNumber)) }
    var message by remember { mutableStateOf("Проводи фишку пальцем к соседней") }
    var resolving by remember { mutableStateOf(false) }
    var rewardGranted by remember(levelNumber) { mutableStateOf(false) }

    MaterialTheme(colorScheme = lightColorScheme(primary = MatchRose, background = MatchCream)) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(MatchPlum, MatchBerry, MatchRose, MatchCream))
            )
        ) {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onClose) { Text("Назад", color = Color.White) }
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Сад чудес · уровень $levelNumber", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("Собирай 3 и больше, создавай каскады", color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .97f)),
                    elevation = CardDefaults.cardElevation(10.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🏆 ${state.score}", color = MatchPlum, fontWeight = FontWeight.Black)
                            Text("Ходы: ${state.movesLeft}", color = MatchRose, fontWeight = FontWeight.Black)
                            Text("Каскад ×${state.combo}", color = MatchPlum, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (state.score.toFloat() / state.level.targetScore).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                            color = MatchRose,
                            trackColor = MatchBlush
                        )
                        Text("Цель: ${state.level.targetScore}", color = Color(0xFF765466), fontSize = 12.sp, modifier = Modifier.align(Alignment.End))
                    }
                }

                Spacer(Modifier.height(10.dp))
                MatchBoard(
                    state = state,
                    enabled = !state.finished && !resolving,
                    onSwap = { first, second ->
                        val result = Match3Engine.swap(state, first, second, Random.Default)
                        if (!result.accepted) {
                            message = "Так совпадения не получится — попробуй другой ход"
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else {
                            resolving = true
                            state = result.state
                            message = when {
                                result.cascades >= 3 -> "Невероятный каскад ×${result.cascades}! ✨"
                                result.cascades == 2 -> "Двойной каскад!"
                                result.removed >= 5 -> "Большая комбинация!"
                                else -> "Отличный ход"
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch { delay(260); resolving = false }
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))
                Text(message, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))

                when {
                    state.won -> {
                        val reward = 35 + levelNumber * 8 + state.bestCombo * 4
                        LaunchedEffect(levelNumber, rewardGranted) {
                            if (!rewardGranted) {
                                rewardGranted = true
                                progressStore.completeGame(reward)
                            }
                        }
                        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MatchGold)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Сад расцвёл! 🌷", color = MatchPlum, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                Text("+$reward любви · лучший каскад ×${state.bestCombo}", color = MatchPlum)
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        levelNumber++
                                        prefs.edit().putInt("level", levelNumber).apply()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MatchRose)
                                ) { Text("Следующий уровень") }
                            }
                        }
                    }
                    state.finished -> Button(
                        onClick = { state = Match3Engine.newState(levelNumber); message = "Новый сад готов — ищи сильные ходы" },
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) { Text("Попробовать уровень снова") }
                    else -> Text(
                        difficultyText(levelNumber),
                        color = Color.White.copy(alpha = .82f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchBoard(
    state: Match3State,
    enabled: Boolean,
    onSwap: (Int, Int) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        val maxBoard = minOf(maxWidth, maxHeight, 440.dp)
        Column(
            Modifier.size(maxBoard).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .96f)).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(state.level.rows) { row ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(state.level.columns) { column ->
                        val index = row * state.level.columns + column
                        MatchTile(
                            gem = state.board[index],
                            enabled = enabled,
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            onDirection = { dx, dy ->
                                val horizontal = abs(dx) > abs(dy)
                                val target = when {
                                    horizontal && dx > 0 && column < state.level.columns - 1 -> index + 1
                                    horizontal && dx < 0 && column > 0 -> index - 1
                                    !horizontal && dy > 0 && row < state.level.rows - 1 -> index + state.level.columns
                                    !horizontal && dy < 0 && row > 0 -> index - state.level.columns
                                    else -> index
                                }
                                if (target != index) onSwap(index, target)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchTile(
    gem: Gem,
    enabled: Boolean,
    modifier: Modifier,
    onDirection: (Float, Float) -> Unit
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (dragging) 1.12f else 1f,
        animationSpec = spring(dampingRatio = .55f, stiffness = 520f),
        label = "tile-scale"
    )
    Box(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(gemBackground(gem))
            .pointerInput(enabled, gem) {
                if (enabled) detectDragGestures(
                    onDragStart = { dragging = true; dragX = 0f; dragY = 0f },
                    onDragEnd = {
                        dragging = false
                        if (abs(dragX) + abs(dragY) > 18f) onDirection(dragX, dragY)
                        dragX = 0f; dragY = 0f
                    },
                    onDragCancel = { dragging = false; dragX = 0f; dragY = 0f },
                    onDrag = { change, amount ->
                        change.consume()
                        dragX += amount.x
                        dragY += amount.y
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(gem.emoji, fontSize = 25.sp)
    }
}

private fun gemBackground(gem: Gem): Color = when (gem) {
    Gem.HEART -> Color(0xFFFFD6E4)
    Gem.FLOWER -> Color(0xFFFFE8C9)
    Gem.STAR -> Color(0xFFFFF2B5)
    Gem.GIFT -> Color(0xFFDCCBFF)
    Gem.BERRY -> Color(0xFFFFC9CE)
    Gem.MOON -> Color(0xFFCFE3FF)
}

private fun difficultyText(level: Int): String = when {
    level <= 3 -> "Начало путешествия: больше ходов и понятные комбинации"
    level <= 7 -> "Сложность растёт: цель выше, а ходов становится меньше"
    level <= 12 -> "Ищи каскады заранее — одиночных совпадений уже недостаточно"
    else -> "Мастерский сад: цени каждый ход и строй цепочки комбинаций"
}
