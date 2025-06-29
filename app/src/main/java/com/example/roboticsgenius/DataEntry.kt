// app/src/main/java/com/example/roboticsgenius/DataEntry.kt
package com.example.roboticsgenius

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_entries")
data class DataEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val activityId: Int,
    val date: String, // Stored as "YYYY-MM-DD"
    val value: String // Can store text, a number, "true"/"false", or comma-separated options
)