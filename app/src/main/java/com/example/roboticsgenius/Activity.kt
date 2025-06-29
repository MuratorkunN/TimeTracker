package com.example.roboticsgenius

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize // Annotation makes the class Parcelable
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
    @ColumnInfo(index = true, defaultValue = "1")
    val dataSetId: Int,
    @ColumnInfo(defaultValue = "1")
    val isTimeTrackerActivity: Boolean = true,
    @ColumnInfo(defaultValue = "Text")
    val dataType: String = "Text",
    val dataOptions: String? = null,
    @ColumnInfo(defaultValue = "0")
    val decimalPlaces: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val isSingleSelection: Boolean = false
) : Parcelable // Implement the interface