// app/src/main/java/com/example/roboticsgenius/DataEntryDao.kt
package com.example.roboticsgenius

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DataEntryDao {
    @Upsert
    suspend fun upsert(dataEntry: DataEntry)

    @Query("SELECT * FROM data_entries WHERE date = :date")
    fun getEntriesForDate(date: String): Flow<List<DataEntry>>

    // THIS IS THE FIX: This function now returns a Flow, making it compatible with `combine`.
    @Query("SELECT * FROM data_entries WHERE date BETWEEN :startDate AND :endDate")
    fun getEntriesInDateRange(startDate: String, endDate: String): Flow<List<DataEntry>>

    @Query("DELETE FROM data_entries WHERE activityId = :activityId")
    suspend fun deleteEntriesForActivity(activityId: Int)

    @Query("""
        SELECT value FROM data_entries
        WHERE activityId = :activityId AND date BETWEEN :startDate AND :endDate
        GROUP BY value
        ORDER BY COUNT(value) DESC, MAX(date) DESC
        LIMIT 5
    """)
    suspend fun getMostFrequentValues(activityId: Int, startDate: String, endDate: String): List<String>
}