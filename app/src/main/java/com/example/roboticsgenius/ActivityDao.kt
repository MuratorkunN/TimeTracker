package com.example.roboticsgenius

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Insert
    suspend fun insert(activity: Activity)

    @Query("SELECT * FROM activities ORDER BY name ASC")
    fun getAllActivities(): Flow<List<Activity>>
}