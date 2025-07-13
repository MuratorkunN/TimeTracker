package com.example.roboticsgenius

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var title: String,
    var content: String,
    val color: String,
    // THE FIX: Changed from val to var to allow modification
    var lastModified: Long
) : Parcelable