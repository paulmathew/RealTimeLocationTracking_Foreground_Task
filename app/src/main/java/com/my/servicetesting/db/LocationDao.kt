package com.my.servicetesting.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocationDao {
    @Query("SELECT * from realtime_location ORDER BY id ASC")
    fun getAllLocations(): LiveData<List<LocationData>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(location: LocationData)

    @Query("SELECT COUNT(id) FROM realtime_location")
    fun getCount(): Int
}