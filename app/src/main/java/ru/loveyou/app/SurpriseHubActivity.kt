package ru.loveyou.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HubPlum = Color(0xFF4D1838)
private val HubBerry = Color(0xFF8E2D56)
private val HubRose = Color(0xFFF05283)
private val HubCream = Color(0xFFFFF8FB)

class SurpriseHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SurpriseHub() }
    }
}

@Composable
private fun SurpriseHub() {
    val context = LocalContext.current
    val progressStore = remember { ProgressStore(context) }
    val progress by progressStore.progress.collectAsState(initial = GameProgress())
    val matchLevel = context.getSharedPreferences("match3_progress", 0).getInt("level", 1)
    val tetrisBest = context.getSharedPreferences("falling_hearts_progress", 0).getInt("best", 0)
    val rescueLevel = context.getSharedPreferences("rescue_progress", 0).getInt("level", 1)
    val memoryLevel = context.getSharedPreferences("memory_progress", 0).getInt("level", 1)
    val puzzleLevel = context.getSharedPreferences("sliding_puzzle_progress", 0).getInt("level", 1)
    val transition = rememberInfiniteTransition(label = "hub")
    val pulse by transition.animateFloat(.96f, 1.06f, infiniteRepeatable(tween(950), RepeatMode.Reverse), label = "pulse")

    MaterialTheme(colorScheme = lightColorScheme(primary = HubRose, background = HubCream)) {
        LazyColumn(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(HubPlum, HubBerry, HubRose, HubCream))).safeDrawingPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text("СЮРПРИЗ", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = 3.sp)
                    Text("Приключение, которое становится сложнее вместе с тобой", color = Color.White.copy(alpha = .84f))
                }
            }
            item {
                Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .97f)), elevation = CardDefaults.cardElevation(12.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❤️", fontSize = 70.sp, modifier = Modifier.scale(pulse))
                        Text("${progress.love} любви", color = HubPlum, fontSize = 29.sp, fontWeight = FontWeight.Black)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            HubStat("⭐", progress.masteryStars, "мастерство")
                            HubStat("🔥", progress.heartBestCombo, "комбо")
                            HubStat("🎁", progress.surprises, "сюрпризы")
                        }
                    }
                }
            }
            item { AdventureCard("💓", "Живое сердце", "Ритм, точность, энергия и комбо · уровень ${progress.heartLevel}", "Играть в ритм") { context.startActivity(Intent(context, SurpriseV4Activity::class.java)) } }
            item { AdventureCard("🌷", "Сад чудес", "Три в ряд со свайпами и каскадами · уровень $matchLevel", "Открыть сад") { context.startActivity(Intent(context, Match3Activity::class.java)) } }
            item { AdventureCard("💞", "Падающие сердца", "Фигуры, линии, комбо и растущая скорость · рекорд $tetrisBest", "Начать падение") { context.startActivity(Intent(context, TetrisActivity::class.java)) } }
            item { AdventureCard("🗝️", "Спаси сердце", "Гарантированно проходимый лабиринт с управлением пальцем · уровень $rescueLevel", "Начать спасение") { context.startActivity(Intent(context, RescueActivity::class.java)) } }
            item { AdventureCard("💌", "Тёплые пары", "Память, предпросмотр, лимит ошибок и растущее поле · уровень $memoryLevel", "Собрать воспоминания") { context.startActivity(Intent(context, MemoryActivity::class.java)) } }
            item { AdventureCard("🧩", "Собери послание", "Решаемая сенсорная головоломка со сдвигом целых линий · уровень $puzzleLevel", "Собрать послание") { context.startActivity(Intent(context, SlidingPuzzleActivity::class.java)) } }
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .18f))) {
                    Text("Основные новые приключения собраны. Следующий этап — полировка анимаций, баланс наград и общая система сюрпризов.", color = Color.White, modifier = Modifier.padding(15.dp))
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}

@Composable
private fun AdventureCard(emoji: String, title: String, subtitle: String, button: String, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .97f)), elevation = CardDefaults.cardElevation(7.dp)) {
        Column(Modifier.fillMaxWidth().padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 42.sp)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = HubPlum, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                    Text(subtitle, color = Color(0xFF765466), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = HubRose), shape = RoundedCornerShape(17.dp)) { Text(button, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
private fun HubStat(emoji: String, value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 21.sp)
        Text("$value", color = HubPlum, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(label, color = Color(0xFF866274), fontSize = 10.sp)
    }
}
