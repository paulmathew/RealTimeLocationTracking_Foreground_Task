package com.my.servicetesting.db

import android.content.Context

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [LocationData::class],version = 2)
abstract class LocationDataBase :RoomDatabase()
{

    abstract fun LocationDao():LocationDao
    companion object {

        @Volatile
        private var INSTANCE: LocationDataBase? = null

        fun getDatabase(context: Context): LocationDataBase? {
            if (INSTANCE == null) {
                synchronized(LocationDataBase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            LocationDataBase::class.java, "locationDB"
                        )
                            .build()
                    }
                }
            }
            return INSTANCE
        }
    }
}