package ru.loveyou.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LoveYouScreen()
            }
        }
    }
}

@Composable
private fun LoveYouScreen() {
    var taps by remember { mutableIntStateOf(0) }
    val heartScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        heartScale.animateTo(
            targetValue = 1.09f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(taps) {
        if (taps > 0) {
            heartScale.snapTo(0.82f)
            heartScale.animateTo(1.18f, tween(180))
            heartScale.animateTo(1f, tween(250))
        }
    }

    val message = when {
        taps == 0 -> "Нажми на кнопку — у меня есть кое-что важное"
        taps == 1 -> "Очень-очень люблю ❤️"
        taps == 2 -> "С каждым днём всё сильнее"
        else -> "Ты — моё самое любимое чудо"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF7A1637), Color(0xFFD65A7F), Color(0xFFFFE8EF))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(0.92f),
            shape = RoundedCornerShape(36.dp),
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(164.dp)
                        .scale(heartScale.value)
                        .background(Color(0xFFFFE2EB), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Сердце",
                        tint = Color(0xFFD92F63),
                        modifier = Modifier.size(104.dp)
                    )
                }

                Spacer(Modifier.height(34.dp))

                Text(
                    text = "Я тебя люблю",
                    fontSize = 40.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF6E1532),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = message,
                    fontSize = 19.sp,
                    lineHeight = 27.sp,
                    color = Color(0xFF67424F),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(38.dp))

                Button(
                    onClick = { taps++ },
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCB315F)),
                    modifier = Modifier.height(58.dp)
                ) {
                    Text(
                        text = if (taps == 0) "Нажми меня" else "Ещё любви!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 22.dp)
                    )
                }
            }
        }
    }
}
