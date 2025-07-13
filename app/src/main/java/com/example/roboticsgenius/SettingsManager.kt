package com.example.roboticsgenius

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

object SettingsManager {
    private const val PREFS_NAME = "app_settings"
    private lateinit var prefs: SharedPreferences

    private const val KEY_APP_START_DATE = "key_app_start_date"

    private val _appStartDate = MutableStateFlow(getDefaultAppStartDate())
    val appStartDate: StateFlow<Calendar> = _appStartDate

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _appStartDate.value = getAppStartDateCalendar()
    }

    fun setAppStartDate(calendar: Calendar) {
        prefs.edit().putLong(KEY_APP_START_DATE, calendar.timeInMillis).apply()
        _appStartDate.value = calendar
    }

    fun getAppStartDateCalendar(): Calendar {
        val defaultDate = getDefaultAppStartDate()
        val timestamp = prefs.getLong(KEY_APP_START_DATE, defaultDate.timeInMillis)
        return Calendar.getInstance().apply { timeInMillis = timestamp }
    }

    private fun getDefaultAppStartDate(): Calendar {
        return Calendar.getInstance().apply {
            set(2025, Calendar.MAY, 20)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}