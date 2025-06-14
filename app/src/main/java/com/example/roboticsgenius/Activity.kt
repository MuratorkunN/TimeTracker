// Activity.kt
package com.example.roboticsgenius
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "activities")
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: String,
    val targetDurationSeconds: Int,
    val targetPeriod: String
)