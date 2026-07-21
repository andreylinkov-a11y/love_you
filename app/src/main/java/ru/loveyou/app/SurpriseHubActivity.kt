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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
private val HubBlush = Color(0xFFFFE7F0)

class SurpriseHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SurpriseHub() }
    }
}

@Composable
private fun SurpriseHub() {
    val context = LocalContext.current
    val store = remember { ProgressStore(context) }
    val progress by store.progress.collectAsState(initial = GameProgress())
    val pulse = rememberInfiniteTransition(label = "hub-pulse")
    val heartScale by pulse.animateFloat(
        initialValue = .96f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
        label = "heart-scale"
    )

    MaterialTheme(colorScheme = lightColorScheme(primary = HubRose, background = HubCream)) {
        Column(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(HubPlum, HubBerry, HubRose, HubCream))
            ).safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("СЮРПРИЗ", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Text("Приключение, которое становится сложнее вместе с тобой", color = Color.White.copy(alpha = .84f))

            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .97f)),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("❤️", fontSize = 68.sp, modifier = Modifier.scale(heartScale))
                    Text("${progress.love} любви", color = HubPlum, fontSize = 29.sp, fontWeight = FontWeight.Black)
                    Text("Общий уровень ${progress.level} · ${progress.masteryStars} звёзд мастерства", color = Color(0xFF765466))
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress.levelProgress },
                        modifier = Modifier.fillMaxWidth().height(13.dp).clip(CircleShape),
                        color = HubRose,
                        trackColor = HubBlush
                    )
                }
            }

            AdventureCard(
                emoji = "💓",
                title = "Живое сердце",
                subtitle = "Уровень ${progress.heartLevel} · ритм, точность и комбо",
                button = "Продолжить ритм"
            ) { context.startActivity(Intent(context, SurpriseV4Activity::class.java)) }

            AdventureCard(
                emoji = "💎",
                title = "Три в ряд",
                subtitle = "Уровень ${progress.match3Level} · цели, каскады и ограниченные ходы",
                button = "Открыть поле"
            ) { context.startActivity(Intent(context, Match3Activity::class.java)) }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                HubStat("🔥", maxOf(progress.heartBestCombo, progress.match3BestCascade), "лучший комбо")
                HubStat("🎁", progress.surprises, "сюрпризы")
                HubStat("🎮", progress.gamesPlayed, "прохождения")
            }
        }
    }
}

@Composable
private fun AdventureCard(emoji: String, title: String, subtitle: String, button: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .97f)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 43.sp)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = HubPlum, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                    Text(subtitle, color = Color(0xFF765466), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HubRose)
            ) { Text(button, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
private fun HubStat(emoji: String, value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 23.sp)
        Text("$value", color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
        Text(label, color = Color.White.copy(alpha = .78f), fontSize = 10.sp)
    }
}
