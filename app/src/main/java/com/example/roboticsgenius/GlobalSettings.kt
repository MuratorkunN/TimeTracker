package com.example.roboticsgenius

import java.util.Calendar

object GlobalSettings {
    /**
     * Returns the global start date for the application.
     * No data can be viewed or logged before this date.
     * Currently hardcoded, will be user-configurable later.
     */
    fun getAppStartDate(): Calendar {
        // Hardcoded to May 20, 2025 as requested.
        return Calendar.getInstance().apply {
            set(2025, Calendar.MAY, 20)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    /**
     * Returns a Calendar instance representing the start of today.
     */
    fun getToday(): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}