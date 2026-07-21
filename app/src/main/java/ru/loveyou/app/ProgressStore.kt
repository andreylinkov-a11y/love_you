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

private val Context.dataStore by preferencesDataStore(name = "surprise_progress")

data class GameProgress(
    val love: Int = 0,
    val streak: Int = 0,
    val lastGiftDate: String = "",
    val gamesPlayed: Int = 0,
    val surprises: Int = 0,
    val onboardingSeen: Boolean = false
) {
    val level: Int get() = love / 80 + 1
    val levelProgress: Float get() = (love % 80) / 80f
}

class ProgressStore(private val context: Context) {
    private val loveKey = intPreferencesKey("love")
    private val streakKey = intPreferencesKey("streak")
    private val lastGiftKey = stringPreferencesKey("last_gift")
    private val gamesPlayedKey = intPreferencesKey("games_played")
    private val surprisesKey = intPreferencesKey("surprises")
    private val onboardingSeenKey = booleanPreferencesKey("onboarding_seen")

    val progress: Flow<GameProgress> = context.dataStore.data.map { prefs ->
        GameProgress(
            love = prefs[loveKey] ?: 0,
            streak = prefs[streakKey] ?: 0,
            lastGiftDate = prefs[lastGiftKey] ?: "",
            gamesPlayed = prefs[gamesPlayedKey] ?: 0,
            surprises = prefs[surprisesKey] ?: 0,
            onboardingSeen = prefs[onboardingSeenKey] ?: false
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
                reward = 25 + (nextStreak.coerceAtMost(7) * 3)
                prefs[lastGiftKey] = today
                prefs[loveKey] = (prefs[loveKey] ?: 0) + reward
                prefs[streakKey] = nextStreak
                prefs[surprisesKey] = (prefs[surprisesKey] ?: 0) + 1
            }
        }
        return reward
    }
}
