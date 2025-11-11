package com.example.lab5

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "english_learning_prefs"
        private const val KEY_MODE = "mode" // "learning" or "testing"
        private const val KEY_INTERVAL = "interval_minutes"
        private const val DEFAULT_MODE = "learning"
        private const val DEFAULT_INTERVAL = 30L // 30 минут по умолчанию
    }

    fun getMode(): String {
        return prefs.getString(KEY_MODE, DEFAULT_MODE) ?: DEFAULT_MODE
    }

    fun setMode(mode: String) {
        prefs.edit().putString(KEY_MODE, mode).apply()
    }

    fun getIntervalMinutes(): Long {
        return prefs.getLong(KEY_INTERVAL, DEFAULT_INTERVAL)
    }

    fun setIntervalMinutes(minutes: Long) {
        prefs.edit().putLong(KEY_INTERVAL, minutes).apply()
    }

    fun isLearningMode(): Boolean {
        return getMode() == "learning"
    }
}

