package com.my.servicetesting.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "realtime_location")
data class LocationData(   @PrimaryKey(autoGenerate = true)
                           var id: Int?=null,
                           @ColumnInfo(name = "location") var location: String,
                           @ColumnInfo(name = "dateTime") var dateTime: String,
                           @ColumnInfo(name = "battery") var battery: String
)

