package ru.loveyou.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "surprise_progress")

data class GameProgress(
    val love: Int = 0,
    val streak: Int = 0,
    val lastGiftDate: String = ""
) {
    val level: Int get() = love / 100 + 1
    val levelProgress: Float get() = (love % 100) / 100f
}

class ProgressStore(private val context: Context) {
    private val loveKey = intPreferencesKey("love")
    private val streakKey = intPreferencesKey("streak")
    private val lastGiftKey = stringPreferencesKey("last_gift")

    val progress: Flow<GameProgress> = context.dataStore.data.map { prefs ->
        GameProgress(
            love = prefs[loveKey] ?: 0,
            streak = prefs[streakKey] ?: 0,
            lastGiftDate = prefs[lastGiftKey] ?: ""
        )
    }

    suspend fun addLove(amount: Int) {
        context.dataStore.edit { it[loveKey] = (it[loveKey] ?: 0) + amount }
    }

    suspend fun claimDailyGift(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        var claimed = false
        context.dataStore.edit { prefs ->
            if (prefs[lastGiftKey] != today) {
                prefs[lastGiftKey] = today
                prefs[loveKey] = (prefs[loveKey] ?: 0) + 25
                prefs[streakKey] = (prefs[streakKey] ?: 0) + 1
                claimed = true
            }
        }
        return claimed
    }
}
