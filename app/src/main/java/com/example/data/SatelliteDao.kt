package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SatelliteDao {
    @Query("SELECT * FROM satellites ORDER BY name ASC")
    fun getAllSatellites(): Flow<List<Satellite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSatellite(satellite: Satellite)

    @Delete
    suspend fun deleteSatellite(satellite: Satellite)

    @Query("SELECT COUNT(*) FROM satellites")
    suspend fun getCount(): Int
}
