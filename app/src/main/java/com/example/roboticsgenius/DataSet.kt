// app/src/main/java/com/example/roboticsgenius/DataSet.kt

package com.example.roboticsgenius
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_sets")
data class DataSet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val iconName: String, // We'll store the icon resource name as a string
    val color: String
)