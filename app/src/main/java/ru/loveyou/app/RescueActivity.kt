package ru.loveyou.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor

private val RescuePlum = Color(0xFF4D1838)
private val RescueRose = Color(0xFFF05283)
private val RescueBlush = Color(0xFFFFE7F0)
private val RescueGold = Color(0xFFFFD36E)

class RescueActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RescueGame() }
    }
}

@Composable
private fun RescueGame() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rescue_progress", 0) }
    var level by remember { mutableIntStateOf(prefs.getInt("level", 1)) }
    var maze by remember(level) { mutableStateOf(RescueEngine.generate(level)) }
    var hero by remember(level) { mutableStateOf(maze.start) }
    var trail by remember(level) { mutableStateOf(listOf(maze.start)) }
    var mistakes by remember(level) { mutableIntStateOf(0) }
    val won = hero == maze.goal
    val progress by animateFloatAsState(trail.size.toFloat() / maze.shortestPath.size.coerceAtLeast(1), label = "path")

    MaterialTheme(colorScheme = lightColorScheme(primary = RescueRose)) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFFFFF8FB)).safeDrawingPadding().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Спаси сердце · уровень $level", color = RescuePlum, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text("Проведи героя пальцем по светлой дорожке. Чем дальше, тем больше развилок.", textAlign = TextAlign.Center, color = Color(0xFF765466))
            LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(10.dp))
            Text("Ошибки: $mistakes · кратчайший путь: ${maze.shortestPath.size - 1}", color = RescuePlum, fontWeight = FontWeight.Bold)

            BoxWithConstraints(
                Modifier.fillMaxWidth().weight(1f).background(RescueBlush, RoundedCornerShape(24.dp)).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val side = minOf(maxWidth, maxHeight)
                Canvas(
                    Modifier.size(side).pointerInput(maze, hero, won) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                if (!won) {
                                    val cellSize = size.width.toFloat() / maze.cols
                                    val cell = MazeCell(floor(pos.y / cellSize).toInt(), floor(pos.x / cellSize).toInt())
                                    if (cell != hero && maze.canMove(hero, cell)) { hero = cell; trail = trail + cell }
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                if (!won) {
                                    val cellSize = size.width.toFloat() / maze.cols
                                    val pos = change.position
                                    val cell = MazeCell(floor(pos.y / cellSize).toInt(), floor(pos.x / cellSize).toInt())
                                    if (cell != hero) {
                                        if (maze.canMove(hero, cell)) { hero = cell; trail = trail + cell }
                                        else if (maze.contains(cell) && cell !in maze.open) mistakes++
                                    }
                                }
                            }
                        )
                    }
                ) {
                    val cell = size.width / maze.cols
                    for (r in 0 until maze.rows) for (c in 0 until maze.cols) {
                        val current = MazeCell(r, c)
                        drawRoundRect(
                            color = if (current in maze.open) Color.White else RescuePlum,
                            topLeft = Offset(c * cell + 2f, r * cell + 2f),
                            size = androidx.compose.ui.geometry.Size(cell - 4f, cell - 4f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * .18f)
                        )
                    }
                    if (trail.size > 1) {
                        for (i in 1 until trail.size) {
                            val a = trail[i - 1]; val b = trail[i]
                            drawLine(RescueGold, Offset((a.col + .5f) * cell, (a.row + .5f) * cell), Offset((b.col + .5f) * cell, (b.row + .5f) * cell), cell * .18f, StrokeCap.Round)
                        }
                    }
                    drawCircle(RescueRose, cell * .32f, Offset((maze.goal.col + .5f) * cell, (maze.goal.row + .5f) * cell))
                    drawCircle(RescueGold, cell * .31f, Offset((hero.col + .5f) * cell, (hero.row + .5f) * cell))
                }
            }

            if (won) {
                Text("Сердце спасено! ❤️", color = RescueRose, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Button(onClick = {
                    level++
                    prefs.edit().putInt("level", level).apply()
                }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Следующий уровень", fontWeight = FontWeight.ExtraBold) }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { hero = maze.start; trail = listOf(maze.start); mistakes = 0 }, modifier = Modifier.weight(1f)) { Text("Сначала") }
                    OutlinedButton(onClick = { maze = RescueEngine.generate(level, (System.nanoTime() and 0x7fffffff).toInt()); hero = maze.start; trail = listOf(maze.start); mistakes = 0 }, modifier = Modifier.weight(1f)) { Text("Новый путь") }
                }
            }
        }
    }
}
