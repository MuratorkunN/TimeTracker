// app/src/main/java/com/example/roboticsgenius/Activity.kt

package com.example.roboticsgenius
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "activities",
    foreignKeys = [ForeignKey(
        entity = DataSet::class,
        parentColumns = ["id"],
        childColumns = ["dataSetId"],
        onDelete = ForeignKey.SET_DEFAULT
    )]
)
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: String,
    val targetDurationSeconds: Int,
    val targetPeriod: String,
    val orderIndex: Int,
    @ColumnInfo(index = true, defaultValue = "1") // Add default for migration
    val dataSetId: Int
)