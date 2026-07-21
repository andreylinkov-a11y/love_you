package ru.loveyou.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
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

private val Plum = Color(0xFF54203F)
private val Berry = Color(0xFF8E2D56)
private val Rose = Color(0xFFF05283)
private val Blush = Color(0xFFFFE7F0)
private val Cream = Color(0xFFFFF8FB)
private val Gold = Color(0xFFFFCE67)
private val Mint = Color(0xFF9FE3CC)

private enum class Screen { HOME, RHYTHM, MEMORY, CATCH, PUZZLE, RESCUE, CHOICE, SEQUENCE }

private data class Adventure(
    val screen: Screen,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val unlockAt: Int,
    val reward: Int,
    val hint: String
)

private val adventures = listOf(
    Adventure(Screen.RHYTHM, "Разбуди сердце", "Весёлая игра на скорость", "💓", 0, 18, "За 12 секунд нажми 24 раза"),
    Adventure(Screen.MEMORY, "Тёплые пары", "Найди одинаковые радости", "🌸", 10, 22, "Открывай по две карточки"),
    Adventure(Screen.CATCH, "Лови искры", "Собери сияние", "✨", 25, 24, "Поймай 12 искр"),
    Adventure(Screen.CHOICE, "Выбери добро", "Три маленькие истории", "😊", 45, 26, "Выбирай самый тёплый ответ"),
    Adventure(Screen.PUZZLE, "Собери послание", "Пятнашки с секретом", "🧩", 70, 30, "Поставь числа по порядку"),
    Adventure(Screen.RESCUE, "Спаси сердце", "Пройди лабиринт", "🔐", 100, 32, "Проведи героя к сердцу"),
    Adventure(Screen.SEQUENCE, "Повтори магию", "Запомни вспышки", "🌈", 135, 36, "Повтори последовательность")
)

class SurpriseActivity : ComponentActivity() {
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
    var reward by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var onboarding by remember { mutableStateOf(!progress.onboardingSeen) }

    MaterialTheme(colorScheme = lightColorScheme(primary = Rose, secondary = Plum, background = Cream)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Plum, Berry, Rose, Cream)))) {
            AnimatedContent(targetState = screen, label = "screen") { current ->
                if (current == Screen.HOME) {
                    HomeScreen(
                        progress = progress,
                        onHelp = { onboarding = true },
                        onGift = {
                            scope.launch {
                                val amount = store.claimDailyGift()
                                if (amount > 0) reward = "Сюрприз дня" to amount
                                else notice = "Сегодняшний сюрприз уже открыт. Возвращайся завтра ✨"
                            }
                        },
                        onGame = { game ->
                            if (progress.love >= game.unlockAt) screen = game.screen
                            else notice = "До «${game.title}» осталось ${game.unlockAt - progress.love} любви. Сыграй в открытую игру или забери подарок дня."
                        }
                    )
                } else {
                    val game = adventures.first { it.screen == current }
                    GameFrame(game, onBack = { screen = Screen.HOME }) {
                        val win: () -> Unit = {
                            scope.launch {
                                store.completeGame(game.reward)
                                reward = game.title to game.reward
                                screen = Screen.HOME
                            }
                        }
                        when (current) {
                            Screen.RHYTHM -> RhythmGame(win)
                            Screen.MEMORY -> MemoryGame(win)
                            Screen.CATCH -> CatchGame(win)
                            Screen.PUZZLE -> PuzzleGame(win)
                            Screen.RESCUE -> RescueGame(win)
                            Screen.CHOICE -> ChoiceGame(win)
                            Screen.SEQUENCE -> SequenceGame(win)
                            else -> Unit
                        }
                    }
                }
            }

            notice?.let { text ->
                LaunchedEffect(text) { delay(3400); notice = null }
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
                    shape = RoundedCornerShape(20.dp), color = Plum, shadowElevation = 12.dp
                ) {
                    Text(text, color = Color.White, modifier = Modifier.padding(18.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (onboarding) {
            AlertDialog(
                onDismissRequest = {},
                icon = { Text("🎁", fontSize = 42.sp) },
                title = { Text("Коробка маленьких чудес") },
                text = { Text("Играй, собирай любовь и открывай новые приключения. Прогресс сохраняется, а каждый день внутри ждёт новый подарок.") },
                confirmButton = {
                    Button(onClick = {
                        onboarding = false
                        scope.launch { store.markOnboardingSeen() }
                    }) { Text("Начать приключение") }
                }
            )
        }

        reward?.let { (title, amount) ->
            AlertDialog(
                onDismissRequest = { reward = null },
                icon = { Text("🎉", fontSize = 50.sp) },
                title = { Text("Сюрприз раскрыт!") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(title, color = Plum, fontWeight = FontWeight.ExtraBold)
                        Text("+$amount любви", color = Rose, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("Сердце стало ярче. Возможно, открылось новое приключение ✨", textAlign = TextAlign.Center)
                    }
                },
                confirmButton = { Button(onClick = { reward = null }) { Text("Продолжить") } }
            )
        }
    }
}

@Composable
private fun HomeScreen(progress: GameProgress, onHelp: () -> Unit, onGift: () -> Unit, onGame: (Adventure) -> Unit) {
    val pulse = rememberInfiniteTransition(label = "heart")
    val scale by pulse.animateFloat(.95f, 1.08f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")

    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("СЮРПРИЗ", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                    Text("Коробка маленьких чудес", color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
                }
                IconButton(onClick = onHelp) { Icon(Icons.Default.Help, null, tint = Color.White) }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(30.dp), color = Color.White.copy(alpha = .96f), shadowElevation = 14.dp) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("❤️", fontSize = 68.sp, modifier = Modifier.scale(scale))
                    Text("Сердце наполняется", color = Plum, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Уровень ${progress.level} · ${progress.love} любви", color = Color(0xFF765466))
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress.levelProgress },
                        modifier = Modifier.fillMaxWidth().height(14.dp).clip(CircleShape),
                        color = Rose, trackColor = Blush
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Stat("🎮", progress.gamesPlayed, "игр")
                        Stat("🎁", progress.surprises, "сюрпризов")
                        Stat("🔥", progress.streak, "дней")
                    }
                }
            }
        }
        item {
            Button(
                onClick = onGift,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) {
                Text("🎁  Открыть сюрприз дня", color = Plum, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
        item {
            val next = adventures.firstOrNull { progress.love < it.unlockAt }
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = .18f)) {
                Text(
                    next?.let { "Следующая игра «${it.title}» откроется через ${it.unlockAt - progress.love} любви" }
                        ?: "Все приключения открыты — собирай коллекцию сюрпризов!",
                    color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(15.dp)
                )
            }
        }
        item { Text("Приключения", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
        items(count = adventures.size) { index ->
            val game = adventures[index]
            val unlocked = progress.love >= game.unlockAt
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onGame(game) },
                shape = RoundedCornerShape(24.dp),
                color = if (unlocked) Color.White.copy(alpha = .96f) else Color.White.copy(alpha = .70f),
                shadowElevation = if (unlocked) 6.dp else 0.dp
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (unlocked) game.emoji else "🔒", fontSize = 36.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(game.title, color = Plum, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        Text(if (unlocked) game.subtitle else "Откроется на ${game.unlockAt} любви", color = Color(0xFF765466), fontSize = 13.sp)
                    }
                    Text("+${game.reward}", color = Rose, fontWeight = FontWeight.Black)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun Stat(emoji: String, value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 21.sp)
        Text("$value", color = Plum, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(label, color = Color(0xFF866274), fontSize = 11.sp)
    }
}

@Composable
private fun GameFrame(game: Adventure, onBack: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text(game.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(game.hint, color = Color.White.copy(alpha = .78f), fontSize = 12.sp)
            }
            Text("+${game.reward} ❤️", color = Gold, fontWeight = FontWeight.Bold)
        }
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(28.dp), color = Color.White.copy(alpha = .97f), shadowElevation = 12.dp
        ) { content() }
    }
}

@Composable
private fun RhythmGame(onWin: () -> Unit) {
    var taps by remember { mutableIntStateOf(0) }
    var time by remember { mutableIntStateOf(12) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(running) { if (running) while (time > 0) { delay(1000); time-- } }
    val won = time == 0 && taps >= 24
    Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        Text(if (!running) "Готов разбудить сердце?" else "$time сек. · $taps / 24", color = Plum, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text("💗", fontSize = (82 + (taps % 4) * 8).sp, modifier = Modifier.clickable(enabled = running && time > 0) { taps++ })
        when {
            !running -> Button(onClick = { running = true }) { Text("Начать") }
            won -> Button(onClick = onWin) { Text("Забрать сияние") }
            time == 0 -> Button(onClick = { taps = 0; time = 12; running = true }) { Text("Ещё попытка") }
            else -> Text("Нажимай быстро — каждое касание оживляет сердце", textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MemoryGame(onWin: () -> Unit) {
    val symbols = remember { listOf("🌸", "🍓", "☕", "🌙", "🐾", "🎵").flatMap { listOf(it, it) }.shuffled() }
    var opened by remember { mutableStateOf(setOf<Int>()) }
    var matched by remember { mutableStateOf(setOf<Int>()) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(opened) {
        if (opened.size == 2) {
            busy = true; delay(550)
            val pair = opened.toList()
            if (symbols[pair[0]] == symbols[pair[1]]) matched = matched + opened
            opened = emptySet(); busy = false
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().padding(12.dp)) {
        val boardSize = minOf(maxWidth - 8.dp, maxHeight - 82.dp, 390.dp)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Text("Найдено пар: ${matched.size / 2} / 6", color = Plum, fontWeight = FontWeight.Bold)
            LazyVerticalGrid(GridCells.Fixed(3), modifier = Modifier.size(boardSize), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(symbols.indices.toList()) { i ->
                    val visible = i in opened || i in matched
                    Surface(
                        modifier = Modifier.aspectRatio(1f).clickable(enabled = !busy && i !in matched && i !in opened && opened.size < 2) { opened = opened + i },
                        shape = RoundedCornerShape(16.dp), color = if (visible) Blush else Plum
                    ) { Box(contentAlignment = Alignment.Center) { Text(if (visible) symbols[i] else "?", fontSize = 30.sp, color = if (visible) Plum else Color.White, fontWeight = FontWeight.Black) } }
                }
            }
            if (matched.size == symbols.size) Button(onClick = onWin) { Text("Все пары вместе!") }
            else Text("Подсказка: сначала запоминай углы", fontSize = 12.sp)
        }
    }
}

@Composable
private fun CatchGame(onWin: () -> Unit) {
    var caught by remember { mutableIntStateOf(0) }
    var x by remember { mutableFloatStateOf(.5f) }
    var y by remember { mutableFloatStateOf(.5f) }
    Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Искры: $caught / 12", color = Plum, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, modifier = Modifier.padding(8.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)).background(Blush)) {
            if (caught < 12) {
                Text("✨", fontSize = 50.sp, modifier = Modifier.offset(x = (maxWidth - 64.dp) * x, y = (maxHeight - 64.dp) * y).clickable {
                    caught++; x = Random.nextFloat(); y = Random.nextFloat()
                })
            } else {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Небо сияет!", color = Plum, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Button(onClick = onWin) { Text("Собрать искры") }
                }
            }
        }
    }
}

private fun shuffledPuzzle(): List<Int> {
    val board = (1..8).toMutableList().apply { add(0) }
    repeat(90) {
        val zero = board.indexOf(0); val row = zero / 3; val col = zero % 3
        val moves = mutableListOf<Int>()
        if (row > 0) moves += zero - 3
        if (row < 2) moves += zero + 3
        if (col > 0) moves += zero - 1
        if (col < 2) moves += zero + 1
        val swap = moves.random(); board[zero] = board[swap]; board[swap] = 0
    }
    return board
}

@Composable
private fun PuzzleGame(onWin: () -> Unit) {
    var board by remember { mutableStateOf(shuffledPuzzle()) }
    val solved = board == listOf(1, 2, 3, 4, 5, 6, 7, 8, 0)
    BoxWithConstraints(Modifier.fillMaxSize().padding(14.dp)) {
        val boardSize = minOf(maxWidth - 8.dp, maxHeight - 86.dp, 380.dp)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Text(if (solved) "Послание собрано ❤️" else "Поставь числа по порядку", color = Plum, fontWeight = FontWeight.ExtraBold)
            LazyVerticalGrid(GridCells.Fixed(3), modifier = Modifier.size(boardSize), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(board.indices.toList()) { i ->
                    val n = board[i]
                    Surface(
                        modifier = Modifier.aspectRatio(1f).clickable(enabled = n != 0) {
                            val z = board.indexOf(0)
                            if (abs(i / 3 - z / 3) + abs(i % 3 - z % 3) == 1) board = board.toMutableList().also { it[z] = n; it[i] = 0 }
                        },
                        shape = RoundedCornerShape(17.dp), color = if (n == 0) Color.Transparent else Rose
                    ) { Box(contentAlignment = Alignment.Center) { if (n != 0) Text("$n", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black) } }
                }
            }
            if (solved) Button(onClick = onWin) { Text("Открыть послание") }
            else TextButton(onClick = { board = shuffledPuzzle() }) { Text("Перемешать") }
        }
    }
}

@Composable
private fun RescueGame(onWin: () -> Unit) {
    val walls = remember { setOf(1, 3, 5, 8, 10, 12, 15, 17, 19, 22, 24, 26, 29, 31, 33, 36, 38, 40, 43, 45) }
    var hero by remember { mutableIntStateOf(42) }
    val goal = 6
    fun move(delta: Int) {
        val next = hero + delta
        if (next in 0..48 && next !in walls && !(delta == 1 && hero % 7 == 6) && !(delta == -1 && hero % 7 == 0)) hero = next
    }
    BoxWithConstraints(Modifier.fillMaxSize().padding(12.dp)) {
        val boardSize = minOf(maxWidth - 8.dp, maxHeight - 118.dp, 350.dp)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Text(if (hero == goal) "Сердце свободно!" else "Найди дорогу к сердцу", color = Plum, fontWeight = FontWeight.ExtraBold)
            LazyVerticalGrid(GridCells.Fixed(7), modifier = Modifier.size(boardSize)) {
                items((0..48).toList()) { i ->
                    Box(Modifier.aspectRatio(1f).padding(2.dp).clip(RoundedCornerShape(7.dp)).background(when { i in walls -> Plum; i == hero -> Gold; i == goal -> Rose; else -> Blush }), contentAlignment = Alignment.Center) {
                        Text(when (i) { hero -> "🙂"; goal -> "❤️"; else -> "" }, fontSize = 18.sp)
                    }
                }
            }
            if (hero == goal) Button(onClick = onWin) { Text("Забрать спасённое сердце") }
            else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { move(-7) }) { Icon(Icons.Default.KeyboardArrowUp, null) }
                Row {
                    IconButton(onClick = { move(-1) }) { Icon(Icons.Default.KeyboardArrowLeft, null) }
                    IconButton(onClick = { move(7) }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                    IconButton(onClick = { move(1) }) { Icon(Icons.Default.KeyboardArrowRight, null) }
                }
            }
        }
    }
}

@Composable
private fun ChoiceGame(onWin: () -> Unit) {
    val stories = remember { listOf(
        Triple("Кто-то грустит. Что подарить?", "Тёплые слова", "Список дел"),
        Triple("За окном дождь. Лучший план?", "Чай и уют", "Считать лужи"),
        Triple("Друг ошибся. Что сказать?", "Я рядом", "Ну и дела")
    ) }
    var index by remember { mutableIntStateOf(0) }
    var smiles by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        if (index < stories.size) {
            val story = stories[index]
            Text("История ${index + 1} из ${stories.size}", color = Rose, fontWeight = FontWeight.Bold)
            Text(story.first, color = Plum, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, textAlign = TextAlign.Center)
            Button(onClick = { smiles++; index++ }, modifier = Modifier.fillMaxWidth()) { Text("😊  ${story.second}") }
            TextButton(onClick = { index++ }, modifier = Modifier.fillMaxWidth()) { Text(story.third) }
        } else {
            Text("Улыбок собрано: $smiles", color = Plum, fontWeight = FontWeight.Black, fontSize = 23.sp)
            Button(onClick = onWin) { Text("Сохранить улыбки") }
        }
    }
}

@Composable
private fun SequenceGame(onWin: () -> Unit) {
    val colors = listOf(Rose, Gold, Mint, Berry)
    var sequence by remember { mutableStateOf(listOf(Random.nextInt(4))) }
    var input by remember { mutableStateOf(emptyList<Int>()) }
    var showing by remember { mutableStateOf(true) }
    var flash by remember { mutableIntStateOf(-1) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(sequence, showing) {
        if (showing) {
            delay(500)
            sequence.forEach { i -> flash = i; delay(420); flash = -1; delay(180) }
            showing = false
        }
    }
    Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        Text("Раунд ${sequence.size} / 5", color = Plum, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text(when { failed -> "Почти! Попробуй ещё раз"; showing -> "Смотри внимательно…"; else -> "Повтори последовательность" })
        LazyVerticalGrid(GridCells.Fixed(2), modifier = Modifier.size(280.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items((0..3).toList()) { i ->
                Surface(
                    modifier = Modifier.aspectRatio(1f).scale(if (flash == i) 1.08f else 1f).clickable(enabled = !showing && !failed) {
                        val next = input + i
                        if (next.last() != sequence[next.lastIndex]) failed = true
                        else if (next.size == sequence.size) {
                            if (sequence.size >= 5) onWin()
                            else { sequence = sequence + Random.nextInt(4); input = emptyList(); showing = true }
                        } else input = next
                    },
                    shape = RoundedCornerShape(24.dp), color = if (flash == i) Color.White else colors[i], shadowElevation = 7.dp
                ) { Box(contentAlignment = Alignment.Center) { if (flash == i) Text("✨", fontSize = 38.sp) } }
            }
        }
        if (failed) Button(onClick = { sequence = listOf(Random.nextInt(4)); input = emptyList(); failed = false; showing = true }) { Text("Начать заново") }
        else Text("Введено: ${input.size} из ${sequence.size}", color = Rose, fontWeight = FontWeight.Bold)
    }
}
