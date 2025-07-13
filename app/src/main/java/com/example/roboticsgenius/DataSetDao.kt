package com.example.roboticsgenius

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DataSetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dataSet: DataSet)

    @Query("SELECT * FROM data_sets ORDER BY id ASC")
    fun getAllDataSets(): Flow<List<DataSet>>

    @Query("DELETE FROM data_sets WHERE id = :dataSetId")
    suspend fun deleteDataSetById(dataSetId: Int)

    @Query("SELECT id FROM activities WHERE dataSetId = :dataSetId")
    suspend fun getActivityIdsForDataSet(dataSetId: Int): List<Int>

    @Query("DELETE FROM activities WHERE dataSetId = :dataSetId")
    suspend fun deleteActivitiesForDataSet(dataSetId: Int)

    @Query("DELETE FROM time_log_entries WHERE activityId IN (:activityIds)")
    suspend fun deleteTimeLogsForActivities(activityIds: List<Int>)

    @Query("DELETE FROM data_entries WHERE activityId IN (:activityIds)")
    suspend fun deleteDataEntriesForActivities(activityIds: List<Int>)

    @Transaction
    suspend fun deleteDataSetAndChildren(dataSetId: Int) {
        val activityIds = getActivityIdsForDataSet(dataSetId)
        if (activityIds.isNotEmpty()) {
            deleteTimeLogsForActivities(activityIds)
            deleteDataEntriesForActivities(activityIds)
        }
        deleteActivitiesForDataSet(dataSetId)
        deleteDataSetById(dataSetId)
    }
}