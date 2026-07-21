package ru.loveyou.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val Context.dataStore by preferencesDataStore(name = "surprise_progress")

data class GameProgress(
    val love: Int = 0,
    val streak: Int = 0,
    val lastGiftDate: String = "",
    val gamesPlayed: Int = 0,
    val surprises: Int = 0,
    val onboardingSeen: Boolean = false,
    val heartLevel: Int = 1,
    val heartBestCombo: Int = 0,
    val heartPerfectRuns: Int = 0,
    val masteryStars: Int = 0
) {
    val level: Int get() = love / 150 + 1
    val levelProgress: Float get() = (love % 150) / 150f
    val nextMilestone: Int get() = level * 150
}

data class HeartRunResult(
    val level: Int,
    val accuracy: Float,
    val bestCombo: Int,
    val perfectHits: Int,
    val reward: Int,
    val stars: Int
)

class ProgressStore(private val context: Context) {
    private val loveKey = intPreferencesKey("love")
    private val streakKey = intPreferencesKey("streak")
    private val lastGiftKey = stringPreferencesKey("last_gift")
    private val gamesPlayedKey = intPreferencesKey("games_played")
    private val surprisesKey = intPreferencesKey("surprises")
    private val onboardingSeenKey = booleanPreferencesKey("onboarding_seen")
    private val heartLevelKey = intPreferencesKey("heart_level")
    private val heartBestComboKey = intPreferencesKey("heart_best_combo")
    private val heartPerfectRunsKey = intPreferencesKey("heart_perfect_runs")
    private val masteryStarsKey = intPreferencesKey("mastery_stars")

    val progress: Flow<GameProgress> = context.dataStore.data.map { prefs ->
        GameProgress(
            love = prefs[loveKey] ?: 0,
            streak = prefs[streakKey] ?: 0,
            lastGiftDate = prefs[lastGiftKey] ?: "",
            gamesPlayed = prefs[gamesPlayedKey] ?: 0,
            surprises = prefs[surprisesKey] ?: 0,
            onboardingSeen = prefs[onboardingSeenKey] ?: false,
            heartLevel = prefs[heartLevelKey] ?: 1,
            heartBestCombo = prefs[heartBestComboKey] ?: 0,
            heartPerfectRuns = prefs[heartPerfectRunsKey] ?: 0,
            masteryStars = prefs[masteryStarsKey] ?: 0
        )
    }

    suspend fun completeGame(amount: Int) {
        context.dataStore.edit { prefs ->
            prefs[loveKey] = (prefs[loveKey] ?: 0) + amount
            prefs[gamesPlayedKey] = (prefs[gamesPlayedKey] ?: 0) + 1
            if ((prefs[gamesPlayedKey] ?: 0) % 3 == 0) {
                prefs[surprisesKey] = (prefs[surprisesKey] ?: 0) + 1
            }
        }
    }

    suspend fun completeHeartLevel(
        accuracy: Float,
        bestCombo: Int,
        perfectHits: Int
    ): HeartRunResult {
        val safeAccuracy = accuracy.coerceIn(0f, 1f)
        var result = HeartRunResult(1, safeAccuracy, bestCombo, perfectHits, 0, 0)
        context.dataStore.edit { prefs ->
            val level = prefs[heartLevelKey] ?: 1
            val levelMultiplier = when {
                level <= 3 -> 1.0f
                level <= 7 -> 1.4f
                level <= 12 -> 1.9f
                level <= 20 -> 2.6f
                else -> (2.6f + (level - 20) * 0.08f).coerceAtMost(4.5f)
            }
            val base = 24 + level * 4
            val qualityMultiplier = 0.65f + safeAccuracy * 0.75f
            val comboBonus = (bestCombo * 0.7f).roundToInt()
            val perfectBonus = perfectHits * 2
            val firstClearBonus = 12 + level * 2
            val reward = (base * levelMultiplier * qualityMultiplier).roundToInt() + comboBonus + perfectBonus + firstClearBonus
            val stars = when {
                safeAccuracy >= .94f && bestCombo >= 10 -> 3
                safeAccuracy >= .82f -> 2
                else -> 1
            }

            prefs[loveKey] = (prefs[loveKey] ?: 0) + reward
            prefs[gamesPlayedKey] = (prefs[gamesPlayedKey] ?: 0) + 1
            prefs[heartLevelKey] = level + 1
            prefs[heartBestComboKey] = maxOf(prefs[heartBestComboKey] ?: 0, bestCombo)
            prefs[masteryStarsKey] = (prefs[masteryStarsKey] ?: 0) + stars
            if (stars == 3) prefs[heartPerfectRunsKey] = (prefs[heartPerfectRunsKey] ?: 0) + 1
            if ((level + 1) % 5 == 0) prefs[surprisesKey] = (prefs[surprisesKey] ?: 0) + 1

            result = HeartRunResult(level, safeAccuracy, bestCombo, perfectHits, reward, stars)
        }
        return result
    }

    suspend fun markOnboardingSeen() {
        context.dataStore.edit { it[onboardingSeenKey] = true }
    }

    suspend fun claimDailyGift(): Int {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = format.format(Date())
        var reward = 0
        context.dataStore.edit { prefs ->
            if (prefs[lastGiftKey] != today) {
                val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
                val continued = prefs[lastGiftKey] == format.format(yesterday)
                val nextStreak = if (continued) (prefs[streakKey] ?: 0) + 1 else 1
                reward = 30 + (nextStreak.coerceAtMost(7) * 5)
                prefs[lastGiftKey] = today
                prefs[loveKey] = (prefs[loveKey] ?: 0) + reward
                prefs[streakKey] = nextStreak
                prefs[surprisesKey] = (prefs[surprisesKey] ?: 0) + 1
            }
        }
        return reward
    }
}
