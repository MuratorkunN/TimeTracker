package com.example.roboticsgenius

import java.util.Calendar
import java.util.concurrent.TimeUnit

object StreakCalculator {

    fun calculateStreak(activity: Activity, logs: List<TimeLogEntry>): Int {
        if (activity.targetDurationSeconds <= 0) return 0

        return when (activity.targetPeriod) {
            "Daily" -> calculateDailyStreak(activity, logs)
            "Weekly" -> calculateWeeklyStreak(activity, logs)
            "Monthly" -> calculateMonthlyStreak(activity, logs)
            else -> 0
        }
    }

    private fun calculateDailyStreak(activity: Activity, logs: List<TimeLogEntry>): Int {
        val logsByDay = groupLogsByCalendarUnit(logs, Calendar.DAY_OF_YEAR)
        val successfulDays = getSuccessfulUnits(logsByDay, activity.targetDurationSeconds)
        if (successfulDays.isEmpty()) return 0

        val today = getStartOf(Calendar.getInstance(), Calendar.DAY_OF_YEAR)
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }

        // Check if today's or yesterday's target was met to start the count
        val lastSuccessfulDay = successfulDays.maxOrNull()!!
        if (lastSuccessfulDay != today.timeInMillis && lastSuccessfulDay != yesterday.timeInMillis) {
            return 0
        }
        // If today not yet met, start from yesterday
        val startDay = if (!successfulDays.contains(today.timeInMillis)) yesterday else today

        return countConsecutive(successfulDays, startDay, Calendar.DAY_OF_YEAR)
    }

    private fun calculateWeeklyStreak(activity: Activity, logs: List<TimeLogEntry>): Int {
        val logsByWeek = groupLogsByCalendarUnit(logs, Calendar.WEEK_OF_YEAR)
        val successfulWeeks = getSuccessfulUnits(logsByWeek, activity.targetDurationSeconds)
        if (successfulWeeks.isEmpty()) return 0

        val thisWeek = getStartOf(Calendar.getInstance(), Calendar.WEEK_OF_YEAR)
        val lastWeek = (thisWeek.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, -1) }

        val lastSuccessfulWeek = successfulWeeks.maxOrNull()!!
        if (lastSuccessfulWeek != thisWeek.timeInMillis && lastSuccessfulWeek != lastWeek.timeInMillis) {
            return 0
        }
        val startWeek = if (!successfulWeeks.contains(thisWeek.timeInMillis)) lastWeek else thisWeek

        return countConsecutive(successfulWeeks, startWeek, Calendar.WEEK_OF_YEAR)
    }

    private fun calculateMonthlyStreak(activity: Activity, logs: List<TimeLogEntry>): Int {
        val logsByMonth = groupLogsByCalendarUnit(logs, Calendar.MONTH)
        val successfulMonths = getSuccessfulUnits(logsByMonth, activity.targetDurationSeconds)
        if (successfulMonths.isEmpty()) return 0

        val thisMonth = getStartOf(Calendar.getInstance(), Calendar.MONTH)
        val lastMonth = (thisMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }

        val lastSuccessfulMonth = successfulMonths.maxOrNull()!!
        if (lastSuccessfulMonth != thisMonth.timeInMillis && lastSuccessfulMonth != lastMonth.timeInMillis) {
            return 0
        }
        val startMonth = if (!successfulMonths.contains(thisMonth.timeInMillis)) lastMonth else thisMonth

        return countConsecutive(successfulMonths, startMonth, Calendar.MONTH)
    }

    private fun groupLogsByCalendarUnit(logs: List<TimeLogEntry>, unit: Int): Map<Long, List<TimeLogEntry>> {
        return logs.groupBy {
            getStartOf(Calendar.getInstance().apply { timeInMillis = it.startTime }, unit).timeInMillis
        }
    }

    private fun getSuccessfulUnits(groupedLogs: Map<Long, List<TimeLogEntry>>, target: Int): Set<Long> {
        return groupedLogs.filterValues { dailyLogs ->
            dailyLogs.sumOf { it.durationInSeconds } >= target
        }.keys
    }

    private fun countConsecutive(successfulUnits: Set<Long>, startUnit: Calendar, unit: Int): Int {
        var streak = 0
        val currentUnit = startUnit.clone() as Calendar
        while (successfulUnits.contains(currentUnit.timeInMillis)) {
            streak++
            currentUnit.add(unit, -1)
        }
        return streak
    }

    private fun getStartOf(cal: Calendar, unit: Int): Calendar {
        val newCal = cal.clone() as Calendar
        when (unit) {
            Calendar.DAY_OF_YEAR -> {
                newCal.set(Calendar.HOUR_OF_DAY, 0)
                newCal.set(Calendar.MINUTE, 0)
                newCal.set(Calendar.SECOND, 0)
                newCal.set(Calendar.MILLISECOND, 0)
            }
            Calendar.WEEK_OF_YEAR -> {
                newCal.firstDayOfWeek = Calendar.MONDAY
                newCal.set(Calendar.DAY_OF_WEEK, newCal.firstDayOfWeek)
                return getStartOf(newCal, Calendar.DAY_OF_YEAR) // chain to reset time
            }
            Calendar.MONTH -> {
                newCal.set(Calendar.DAY_OF_MONTH, 1)
                return getStartOf(newCal, Calendar.DAY_OF_YEAR) // chain to reset time
            }
        }
        return newCal
    }
}