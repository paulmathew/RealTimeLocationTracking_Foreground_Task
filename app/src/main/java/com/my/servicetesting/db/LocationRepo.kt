package com.my.servicetesting.db

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class LocationRepo (application: Application):CoroutineScope
{
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private var locationDao: LocationDao?

    init {
        val db = LocationDataBase.getDatabase(application)
        locationDao = db?.LocationDao()
    }

    fun getLocations() = locationDao?.getAllLocations()

    fun setLocations(locations: LocationData) {
        Log.e("Adding",""+locations)
        launch  { addLocations(locations) }
    }

    fun getNumFiles(): Int? {
        return locationDao?.getCount()
    }
    private suspend fun addLocations(location: LocationData){
        withContext(Dispatchers.IO){
            locationDao?.insert(location)
        }
    }

}