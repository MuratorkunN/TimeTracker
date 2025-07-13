package com.example.roboticsgenius

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val message: String,
    // Full timestamp in millis for the next time the notification should fire.
    // This will be updated for repeating reminders.
    val nextTriggerTime: Long,
    // Bitmask for days of the week. 0 for no repeat.
    // 1=SUN, 2=MON, 4=TUE, 8=WED, 16=THU, 32=FRI, 64=SAT
    val repeatDays: Int,
    val color: String,
    val isEnabled: Boolean = true
) : Parcelable