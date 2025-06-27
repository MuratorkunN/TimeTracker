// app/src/main/java/com/example/roboticsgenius/DataSetDao.kt

package com.example.roboticsgenius

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DataSetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dataSet: DataSet)

    @Query("SELECT * FROM data_sets ORDER BY id ASC")
    fun getAllDataSets(): Flow<List<DataSet>>
}