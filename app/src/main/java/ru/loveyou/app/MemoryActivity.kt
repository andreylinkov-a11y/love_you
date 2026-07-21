package ru.loveyou.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val MemoryPlum = Color(0xFF4D1838)
private val MemoryBerry = Color(0xFF8E2D56)
private val MemoryRose = Color(0xFFF05283)
private val MemoryBlush = Color(0xFFFFE7F0)
private val MemoryCream = Color(0xFFFFF8FB)
private val MemoryGold = Color(0xFFFFD36E)

class MemoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MemoryAdventure { finish() } }
    }
}

@Composable
private fun MemoryAdventure(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("memory_progress", 0) }
    val store = remember { ProgressStore(context) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var levelNumber by remember { mutableIntStateOf(prefs.getInt("level", 1).coerceAtLeast(1)) }
    var state by remember(levelNumber) { mutableStateOf(MemoryEngine.newState(levelNumber)) }
    var preview by remember(levelNumber) { mutableStateOf(true) }
    var secondsLeft by remember(levelNumber) { mutableIntStateOf(state.level.timeSeconds) }
    var resolving by remember { mutableStateOf(false) }
    var rewarded by remember(levelNumber) { mutableStateOf(false) }
    var message by remember { mutableStateOf("Запомни расположение тёплых символов") }

    LaunchedEffect(levelNumber) {
        preview = true
        delay(state.level.previewMillis)
        preview = false
        message = "Открывай карточки парами"
    }
    LaunchedEffect(preview, state.finished, levelNumber) {
        if (!preview && !state.finished) {
            while (secondsLeft > 0 && !state.finished) {
                delay(1000)
                secondsLeft--
            }
            if (secondsLeft <= 0 && !state.finished) state = MemoryEngine.timeout(state)
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = MemoryRose, background = MemoryCream)) {
        Column(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MemoryPlum, MemoryBerry, MemoryRose, MemoryCream))).safeDrawingPadding().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onClose) { Text("Назад", color = Color.White) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Тёплые пары · уровень $levelNumber", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Поле растёт, предпросмотр сокращается", color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .97f))) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("⏱ $secondsLeft", color = MemoryPlum, fontWeight = FontWeight.Black)
                        Text("🔥 ${state.combo}", color = MemoryRose, fontWeight = FontWeight.Black)
                        Text("Ошибки ${state.mistakes}/${state.level.maxMistakes}", color = MemoryPlum, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(7.dp))
                    LinearProgressIndicator(
                        progress = { state.matched.size.toFloat() / state.cards.size },
                        modifier = Modifier.fillMaxWidth().height(11.dp).clip(CircleShape),
                        color = MemoryRose,
                        trackColor = MemoryBlush
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val rows = (state.cards.size + state.level.columns - 1) / state.level.columns
                val gap = 6.dp
                val maxBoard = minOf(maxWidth, maxHeight * state.level.columns / rows)
                Column(
                    Modifier.width(maxBoard).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .96f)).padding(7.dp),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    repeat(rows) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            repeat(state.level.columns) { col ->
                                val index = row * state.level.columns + col
                                if (index < state.cards.size) {
                                    val visible = preview || index in state.open || index in state.matched
                                    MemoryTile(
                                        symbol = state.cards[index].symbol,
                                        visible = visible,
                                        matched = index in state.matched,
                                        modifier = Modifier.weight(1f).aspectRatio(1f),
                                        enabled = !preview && !resolving && !state.finished,
                                        onClick = {
                                            val result = MemoryEngine.flip(state, index)
                                            if (result.accepted) {
                                                state = result.state
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (result.pairReady) {
                                                    resolving = true
                                                    scope.launch {
                                                        delay(620)
                                                        val before = state.matched.size
                                                        state = MemoryEngine.resolvePair(state)
                                                        message = if (state.matched.size > before) "Пара найдена! ✨" else "Не совпало — запомни эти места"
                                                        resolving = false
                                                    }
                                                }
                                            }
                                        }
                                    )
                                } else Spacer(Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }
            Text(if (preview) "Смотри внимательно…" else message, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            when {
                state.won -> {
                    val reward = 30 + levelNumber * 7 + state.bestCombo * 5 + (state.level.maxMistakes - state.mistakes) * 3
                    LaunchedEffect(levelNumber, rewarded) {
                        if (!rewarded) { rewarded = true; store.completeGame(reward) }
                    }
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MemoryGold)) {
                        Column(Modifier.fillMaxWidth().padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Все воспоминания вместе! 💌", color = MemoryPlum, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text("+$reward любви · комбо ${state.bestCombo}", color = MemoryPlum)
                            Button(onClick = { levelNumber++; prefs.edit().putInt("level", levelNumber).apply() }, modifier = Modifier.fillMaxWidth()) { Text("Следующий уровень") }
                        }
                    }
                }
                state.finished -> Button(
                    onClick = { state = MemoryEngine.newState(levelNumber); secondsLeft = state.level.timeSeconds; preview = true; message = "Новая попытка" },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Попробовать снова") }
                else -> Text("Найдено ${state.matched.size / 2} из ${state.level.pairCount} пар", color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MemoryTile(symbol: String, visible: Boolean, matched: Boolean, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(if (visible) 0f else 180f, spring(dampingRatio = .72f, stiffness = 520f), label = "flip")
    Card(
        modifier.graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }.clickable(enabled = enabled && !matched, onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = if (visible) MemoryBlush else MemoryPlum),
        elevation = CardDefaults.cardElevation(if (matched) 1.dp else 5.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (visible) symbol else "✦", fontSize = 27.sp, color = if (visible) MemoryPlum else Color.White)
        }
    }
}
