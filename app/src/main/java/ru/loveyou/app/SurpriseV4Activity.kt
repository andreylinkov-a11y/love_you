package ru.loveyou.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlin.math.roundToInt

private val V4Plum = Color(0xFF4D1838)
private val V4Berry = Color(0xFF8E2D56)
private val V4Rose = Color(0xFFF05283)
private val V4Blush = Color(0xFFFFE7F0)
private val V4Gold = Color(0xFFFFD36E)
private val V4Cream = Color(0xFFFFF8FB)
private val V4Mint = Color(0xFF9FE3CC)

class SurpriseV4Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SurpriseV4App() }
    }
}

private enum class V4Page { HOME, HEART }

@Composable
private fun SurpriseV4App() {
    val context = LocalContext.current
    val store = remember { ProgressStore(context) }
    val progress by store.progress.collectAsState(initial = GameProgress())
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(V4Page.HOME) }
    var result by remember { mutableStateOf<HeartRunResult?>(null) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = V4Rose,
            secondary = V4Berry,
            background = V4Cream
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(V4Plum, V4Berry, V4Rose, V4Cream)))
        ) {
            when (page) {
                V4Page.HOME -> V4Home(
                    progress = progress,
                    onHeart = { page = V4Page.HEART },
                    onLegacyGames = { context.startActivity(Intent(context, SurpriseActivity::class.java)) }
                )

                V4Page.HEART -> LivingHeartGame(
                    level = progress.heartLevel,
                    onBack = { page = V4Page.HOME },
                    onComplete = { accuracy, combo, perfect ->
                        scope.launch {
                            result = store.completeHeartLevel(accuracy, combo, perfect)
                            page = V4Page.HOME
                        }
                    }
                )
            }

            result?.let { run -> HeartResultCard(run = run, onClose = { result = null }) }
        }
    }
}

@Composable
private fun V4Home(
    progress: GameProgress,
    onHeart: () -> Unit,
    onLegacyGames: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "home-heart")
    val scale by pulse.animateFloat(
        initialValue = .96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "home-pulse"
    )

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("СЮРПРИЗ · НОВАЯ ГЛАВА", color = Color.White, fontWeight = FontWeight.Black)
        Text("Теперь игры развиваются вместе с тобой", color = Color.White.copy(alpha = .82f))

        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .97f)),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("❤️", fontSize = 72.sp, modifier = Modifier.scale(scale))
                Text("${progress.love} любви", color = V4Plum, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text("Общий уровень ${progress.level} · до рубежа ${progress.nextMilestone}", color = Color(0xFF765466))
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress.levelProgress },
                    modifier = Modifier.fillMaxWidth().height(14.dp).clip(CircleShape),
                    color = V4Rose,
                    trackColor = V4Blush
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MiniStat("⭐", progress.masteryStars, "мастерство")
                    MiniStat("🔥", progress.heartBestCombo, "лучшее комбо")
                    MiniStat("🎁", progress.surprises, "сюрпризы")
                }
            }
        }

        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .97f)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💓", fontSize = 44.sp)
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("Живое сердце", color = V4Plum, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Уровень ${progress.heartLevel} · ритм становится сложнее", color = Color(0xFF765466))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Попадай в импульс, удерживай энергию и собирай комбо. Награда растёт вместе со сложностью и точностью.",
                    color = Color(0xFF684557)
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onHeart,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = V4Rose)
                ) {
                    Text("Играть · уровень ${progress.heartLevel}", fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Surface(shape = RoundedCornerShape(22.dp), color = Color.White.copy(alpha = .18f)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Дальше", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("Три в ряд, тетрис и новые уровни появятся отдельными качественными этапами.", color = Color.White.copy(alpha = .86f))
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onLegacyGames, modifier = Modifier.fillMaxWidth()) {
                    Text("Открыть прежние мини-игры", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MiniStat(emoji: String, value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 22.sp)
        Text("$value", color = V4Plum, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(label, color = Color(0xFF866274), fontSize = 10.sp)
    }
}

@Composable
private fun LivingHeartGame(
    level: Int,
    onBack: () -> Unit,
    onComplete: (accuracy: Float, bestCombo: Int, perfectHits: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val durationSeconds = (18 + level.coerceAtMost(8)).coerceAtMost(26)
    val pulseMillis = (1080 - (level - 1) * 45).coerceAtLeast(430)
    val targetHits = (12 + level * 2).coerceAtMost(42)
    val timingWindow = (0.30f - level * 0.009f).coerceAtLeast(.13f)

    var running by remember(level) { mutableStateOf(false) }
    var finished by remember(level) { mutableStateOf(false) }
    var secondsLeft by remember(level) { mutableIntStateOf(durationSeconds) }
    var energy by remember(level) { mutableFloatStateOf(.34f) }
    var hits by remember(level) { mutableIntStateOf(0) }
    var misses by remember(level) { mutableIntStateOf(0) }
    var combo by remember(level) { mutableIntStateOf(0) }
    var bestCombo by remember(level) { mutableIntStateOf(0) }
    var perfectHits by remember(level) { mutableIntStateOf(0) }
    var feedback by remember(level) { mutableStateOf("Нажимай, когда кольцо касается сердца") }
    var tapBoost by remember(level) { mutableFloatStateOf(0f) }

    val transition = rememberInfiniteTransition(label = "rhythm")
    val ringPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )
    val idlePulse by transition.animateFloat(
        initialValue = .98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "idle-heart"
    )
    val energyScale by animateFloatAsState(
        targetValue = .82f + energy * .34f + tapBoost,
        animationSpec = spring(dampingRatio = .52f, stiffness = 430f),
        label = "energy-scale"
    )

    LaunchedEffect(running, finished, level) {
        if (running && !finished) {
            var ticks = 0
            while (secondsLeft > 0 && energy > 0f) {
                delay(100)
                ticks++
                energy = (energy - (.0042f + level * .00018f)).coerceAtLeast(0f)
                if (ticks % 10 == 0) secondsLeft = (secondsLeft - 1).coerceAtLeast(0)
            }
            finished = true
            running = false
        }
    }

    val accuracy = if (hits + misses == 0) 0f else hits.toFloat() / (hits + misses)
    val success = finished && hits >= targetHits && energy > 0f

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("Назад") }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Живое сердце · уровень $level", color = V4Plum, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("Скорость растёт, окно точности сужается", color = Color(0xFF765466), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("⏱ $secondsLeft", color = V4Plum, fontWeight = FontWeight.Black)
            Text("🔥 $combo", color = V4Rose, fontWeight = FontWeight.Black)
            Text("🎯 $hits / $targetHits", color = V4Plum, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { energy },
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
            color = if (energy > .28f) V4Mint else V4Rose,
            trackColor = V4Blush
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(running, finished, ringPhase) {
                    detectTapGestures {
                        if (!running || finished) return@detectTapGestures
                        val distance = minOf(ringPhase, 1f - ringPhase) * 2f
                        val quality = 1f - distance
                        val isPerfect = quality >= 1f - timingWindow * .45f
                        val isGood = quality >= 1f - timingWindow

                        if (isGood) {
                            hits++
                            combo++
                            bestCombo = maxOf(bestCombo, combo)
                            if (isPerfect) perfectHits++
                            energy = (energy + if (isPerfect) .095f else .062f).coerceAtMost(1f)
                            feedback = if (isPerfect) "Идеально! ✨" else "Точно!"
                            tapBoost = if (isPerfect) .18f else .11f
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } else {
                            misses++
                            combo = 0
                            energy = (energy - .045f).coerceAtLeast(0f)
                            feedback = "Мимо ритма — дождись импульса"
                            tapBoost = .04f
                        }

                        scope.launch {
                            delay(90)
                            tapBoost = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val ringScale = 1.55f - ringPhase * .55f
            Box(
                Modifier
                    .size(220.dp)
                    .scale(ringScale)
                    .alpha(.18f + (1f - ringPhase) * .45f)
                    .clip(CircleShape)
                    .background(V4Rose.copy(alpha = .20f))
            )
            Text(
                "❤️",
                fontSize = 132.sp,
                modifier = Modifier.scale(if (running) energyScale else idlePulse)
            )
            if (combo >= 4) {
                Text(
                    "КОМБО ×$combo",
                    color = V4Gold,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 34.dp)
                )
            }
        }

        Text(feedback, color = V4Plum, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Точность ${(accuracy * 100).roundToInt()}% · идеально $perfectHits", color = Color(0xFF765466), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))

        when {
            !running && !finished -> Button(
                onClick = {
                    secondsLeft = durationSeconds
                    energy = .42f
                    hits = 0
                    misses = 0
                    combo = 0
                    bestCombo = 0
                    perfectHits = 0
                    feedback = "Лови первый импульс"
                    running = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) { Text("Начать уровень $level", fontWeight = FontWeight.ExtraBold) }

            success -> Button(
                onClick = { onComplete(accuracy, bestCombo, perfectHits) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = V4Rose)
            ) { Text("Забрать награду и открыть уровень ${level + 1}", fontWeight = FontWeight.ExtraBold) }

            finished -> Button(
                onClick = {
                    secondsLeft = durationSeconds
                    energy = .42f
                    hits = 0
                    misses = 0
                    combo = 0
                    bestCombo = 0
                    perfectHits = 0
                    feedback = "Попробуем ещё раз"
                    finished = false
                    running = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) { Text("Повторить · нужно $targetHits точных попаданий") }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun HeartResultCard(run: HeartRunResult, onClose: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .48f)).padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(18.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("💖", fontSize = 64.sp)
                Text("Уровень ${run.level} пройден!", color = V4Plum, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("⭐".repeat(run.stars), fontSize = 30.sp)
                Text("+${run.reward} любви", color = V4Rose, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text(
                    "Точность ${(run.accuracy * 100).roundToInt()}% · комбо ${run.bestCombo} · идеальных ${run.perfectHits}",
                    textAlign = TextAlign.Center
                )
                if ((run.level + 1) % 5 == 0) {
                    Text("🎁 Открыт особый сюрприз за серию уровней!", color = V4Berry, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Продолжить") }
            }
        }
    }
}
