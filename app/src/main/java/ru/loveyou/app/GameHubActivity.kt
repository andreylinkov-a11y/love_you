package ru.loveyou.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class GameHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameHub(
                onHeart = { startActivity(Intent(this, SurpriseV4Activity::class.java)) },
                onMatch3 = { startActivity(Intent(this, Match3Activity::class.java)) },
                onLegacy = { startActivity(Intent(this, SurpriseActivity::class.java)) }
            )
        }
    }
}

@Composable
private fun GameHub(onHeart: () -> Unit, onMatch3: () -> Unit, onLegacy: () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFFF05283))) {
        Column(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF4D1838), Color(0xFFF05283), Color(0xFFFFF8FB))))
                .safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("СЮРПРИЗ", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text("Игровое приключение", color = Color.White.copy(alpha = .85f), fontSize = 18.sp)
            HubCard("💓", "Живое сердце", "Ритм, точность, энергия и комбо.", "Играть", onHeart)
            HubCard("💎", "Три в ряд", "Свайпы, каскады, цели и ограничение ходов.", "Начать уровни", onMatch3)
            Spacer(Modifier.weight(1f))
            Button(onClick = onLegacy, modifier = Modifier.fillMaxWidth()) { Text("Прежние мини-игры") }
        }
    }
}

@Composable
private fun HubCard(emoji: String, title: String, description: String, button: String, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 42.sp)
                Spacer(Modifier.width(12.dp))
                Text(title, color = Color(0xFF4D1838), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(description, color = Color(0xFF684557))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(button) }
        }
    }
}
