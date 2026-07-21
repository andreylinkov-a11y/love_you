package ru.loveyou.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

private val Berry = Color(0xFF7B1E46)
private val Pink = Color(0xFFE94B7A)
private val Cream = Color(0xFFFFF4F7)
private val Gold = Color(0xFFFFC857)

private enum class Screen { HOME, TAP, MEMORY, PUZZLE, RESCUE, CATCH }

private data class GameCard(
    val screen: Screen,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val unlockAt: Int,
    val reward: Int
)

private val games = listOf(
    GameCard(Screen.TAP, "Сердечный ритм", "Нажимай в такт и разбуди сердце", "💓", 0, 15),
    GameCard(Screen.MEMORY, "Тёплые пары", "Найди одинаковые маленькие радости", "🧠", 40, 25),
    GameCard(Screen.CATCH, "Лови искры", "Поймай разбегающиеся искорки любви", "✨", 90, 25),
    GameCard(Screen.PUZZLE, "Собери послание", "Верни все кусочки на свои места", "🧩", 150, 35),
    GameCard(Screen.RESCUE, "Спаси сердце", "Проведи героя через маленький лабиринт", "🔐", 230, 40)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SurpriseApp() }
    }
}

@Composable
private fun SurpriseApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { ProgressStore(context) }
    val progress by store.progress.collectAsState(initial = GameProgress())
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(Screen.HOME) }
    var toast by remember { mutableStateOf<String?>(null) }

    MaterialTheme(
        colorScheme = lightColorScheme(primary = Pink, secondary = Berry, background = Cream)
    ) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Berry, Pink, Cream)))) {
            AnimatedContent(targetState = screen, label = "screen") { current ->
                when (current) {
                    Screen.HOME -> HomeScreen(
                        progress = progress,
                        onGift = {
                            scope.launch {
                                toast = if (store.claimDailyGift()) "Коробка открыта: +25 любви!" else "Сегодняшний сюрприз уже получен"
                            }
                        },
                        onGame = { game ->
                            if (progress.love >= game.unlockAt) screen = game.screen
                            else toast = "Нужно ещё ${game.unlockAt - progress.love} любви"
                        }
                    )
                    else -> GameShell(
                        title = games.first { it.screen == current }.title,
                        onBack = { screen = Screen.HOME }
                    ) {
                        val game = games.first { it.screen == current }
                        val finish: () -> Unit = {
                            scope.launch {
                                store.addLove(game.reward)
                                toast = "Получено +${game.reward} любви!"
                                delay(500)
                                screen = Screen.HOME
                            }
                        }
                        when (current) {
                            Screen.TAP -> TapGame(finish)
                            Screen.MEMORY -> MemoryGame(finish)
                            Screen.PUZZLE -> SlidingPuzzle(finish)
                            Screen.RESCUE -> RescueGame(finish)
                            Screen.CATCH -> CatchGame(finish)
                            else -> Unit
                        }
                    }
                }
            }

            toast?.let { message ->
                LaunchedEffect(message) { delay(2200); toast = null }
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                    shape = RoundedCornerShape(18.dp), color = Berry
                ) {
                    Text(message, color = Color.White, modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(progress: GameProgress, onGift: () -> Unit, onGame: (GameCard) -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("СЮРПРИЗ", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(30.dp), color = Color.White.copy(alpha = .95f), shadowElevation = 14.dp) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val scale by animateFloatAsState(1f + progress.levelProgress * .12f, label = "heart")
                Text("❤️", fontSize = (66 * scale).sp)
                Text("Сердце наполняется", color = Berry, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("Уровень ${progress.level} · ${progress.love} любви", color = Color(0xFF79505F), fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress.levelProgress },
                    modifier = Modifier.fillMaxWidth().height(14.dp).clip(CircleShape),
                    color = Pink, trackColor = Color(0xFFFFD9E4)
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onGift, shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                    Text("🎁  Открыть сюрприз дня", color = Color(0xFF4A3410), fontWeight = FontWeight.ExtraBold)
                }
                Text("Серия: ${progress.streak} дн.", color = Color(0xFF9A6C19), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Маленькие приключения", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(games) { game ->
                val unlocked = progress.love >= game.unlockAt
                Surface(
                    modifier = Modifier.height(150.dp).clickable { onGame(game) },
                    shape = RoundedCornerShape(24.dp),
                    color = if (unlocked) Color.White.copy(alpha = .96f) else Color.White.copy(alpha = .68f)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (unlocked) game.emoji else "🔒", fontSize = 32.sp)
                            Text("+${game.reward}", color = Pink, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(game.title, color = Berry, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text(if (unlocked) game.subtitle else "Откроется на ${game.unlockAt}", color = Color(0xFF79505F), fontSize = 12.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameShell(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        }
        Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(30.dp), color = Color.White.copy(alpha = .96f)) {
            content()
        }
    }
}

@Composable
private fun TapGame(onWin: () -> Unit) {
    var taps by remember { mutableIntStateOf(0) }
    var time by remember { mutableIntStateOf(15) }
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(started) {
        if (started) while (time > 0) { delay(1000); time-- }
    }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        Text(if (!started) "Разбуди сердце" else "Осталось $time сек. · $taps ударов", color = Berry, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("💗", fontSize = (92 + (taps % 3) * 12).sp, modifier = Modifier.clickable(enabled = started && time > 0) { taps++ })
        when {
            !started -> Button(onClick = { started = true }) { Text("Начать") }
            time == 0 && taps >= 30 -> Button(onClick = onWin) { Text("Забрать любовь") }
            time == 0 -> Button(onClick = { taps = 0; time = 15; started = true }) { Text("Ещё попытка · нужно 30") }
            else -> Text("Нажимай на сердце как можно быстрее", textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MemoryGame(onWin: () -> Unit) {
    val symbols = remember { listOf("🌸","🍓","☕","🌙","🐾","🎵").flatMap { listOf(it, it) }.shuffled() }
    var opened by remember { mutableStateOf(setOf<Int>()) }
    var matched by remember { mutableStateOf(setOf<Int>()) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(opened) {
        if (opened.size == 2) {
            busy = true; delay(650)
            val pair = opened.toList()
            if (symbols[pair[0]] == symbols[pair[1]]) matched = matched + opened
            opened = emptySet(); busy = false
        }
    }
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        Text("Найди все тёплые пары", color = Berry, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(390.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(symbols.indices.toList()) { i ->
                val visible = i in opened || i in matched
                Surface(
                    modifier = Modifier.aspectRatio(1f).clickable(enabled = !busy && i !in matched && i !in opened && opened.size < 2) { opened = opened + i },
                    shape = RoundedCornerShape(18.dp), color = if (visible) Color(0xFFFFE4EC) else Berry
                ) { Box(contentAlignment = Alignment.Center) { Text(if (visible) symbols[i] else "?", fontSize = 34.sp, color = Color.White, fontWeight = FontWeight.Bold) } }
            }
        }
        if (matched.size == symbols.size) Button(onClick = onWin) { Text("Все пары вместе!") }
    }
}

private fun shuffledPuzzle(): List<Int> {
    val board = (1..8).toMutableList().apply { add(0) }
    repeat(120) {
        val zero = board.indexOf(0); val row = zero / 3; val col = zero % 3
        val moves = mutableListOf<Int>()
        if (row > 0) moves += zero - 3; if (row < 2) moves += zero + 3
        if (col > 0) moves += zero - 1; if (col < 2) moves += zero + 1
        val swap = moves.random(); board[zero] = board[swap]; board[swap] = 0
    }
    return board
}

@Composable
private fun SlidingPuzzle(onWin: () -> Unit) {
    var board by remember { mutableStateOf(shuffledPuzzle()) }
    val solved = board == listOf(1,2,3,4,5,6,7,8,0)
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        Text("Собери послание по порядку", color = Berry, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.size(330.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(board.indices.toList()) { i ->
                val n = board[i]
                Surface(
                    modifier = Modifier.aspectRatio(1f).clickable(enabled = n != 0) {
                        val z = board.indexOf(0)
                        if (abs(i / 3 - z / 3) + abs(i % 3 - z % 3) == 1) {
                            board = board.toMutableList().also { it[z] = n; it[i] = 0 }
                        }
                    },
                    shape = RoundedCornerShape(18.dp), color = if (n == 0) Color.Transparent else Pink
                ) { Box(contentAlignment = Alignment.Center) { if (n != 0) Text("$n", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black) } }
            }
        }
        if (solved) Button(onClick = onWin) { Text("Послание собрано ❤️") }
        else TextButton(onClick = { board = shuffledPuzzle() }) { Text("Перемешать ещё раз") }
    }
}

@Composable
private fun RescueGame(onWin: () -> Unit) {
    val walls = remember { setOf(1,3,5,8,10,12,15,17,19,22,24,26,29,31,33,36,38,40,43,45) }
    var hero by remember { mutableIntStateOf(42) }
    val goal = 6
    fun move(delta: Int) {
        val next = hero + delta
        if (next in 0..48 && next !in walls && !(delta == 1 && hero % 7 == 6) && !(delta == -1 && hero % 7 == 0)) hero = next
    }
    Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        Text("Доберись до запертого сердца", color = Berry, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.size(315.dp)) {
            items((0..48).toList()) { i ->
                Box(Modifier.aspectRatio(1f).padding(2.dp).clip(RoundedCornerShape(7.dp)).background(when { i in walls -> Berry; i == hero -> Gold; i == goal -> Pink; else -> Color(0xFFFFEDF2) }), contentAlignment = Alignment.Center) {
                    Text(when (i) { hero -> "🙂"; goal -> "❤️"; else -> "" }, fontSize = 20.sp)
                }
            }
        }
        if (hero == goal) Button(onClick = onWin) { Text("Сердце спасено!") }
        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { move(-7) }) { Icon(Icons.Default.KeyboardArrowUp, null) }
            Row { IconButton(onClick = { move(-1) }) { Icon(Icons.Default.KeyboardArrowLeft, null) }; IconButton(onClick = { move(1) }) { Icon(Icons.Default.KeyboardArrowRight, null) } }
            IconButton(onClick = { move(7) }) { Icon(Icons.Default.KeyboardArrowDown, null) }
        }
    }
}

@Composable
private fun CatchGame(onWin: () -> Unit) {
    var caught by remember { mutableIntStateOf(0) }
    var x by remember { mutableFloatStateOf(.5f) }
    var y by remember { mutableFloatStateOf(.5f) }
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Поймано искр: $caught / 15", color = Berry, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(12.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)).background(Color(0xFFFFEDF2))) {
            if (caught < 15) {
                Text("✨", fontSize = 48.sp, modifier = Modifier.offset(x = (maxWidth - 60.dp) * x, y = (maxHeight - 60.dp) * y).clickable {
                    caught++; x = Random.nextFloat(); y = Random.nextFloat()
                })
            } else {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Все искры пойманы!", color = Berry, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Spacer(Modifier.height(16.dp)); Button(onClick = onWin) { Text("Собрать сияние") }
                }
            }
        }
    }
}
