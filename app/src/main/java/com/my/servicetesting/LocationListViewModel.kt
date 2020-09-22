package com.my.servicetesting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.my.servicetesting.db.LocationData
import com.my.servicetesting.db.LocationRepo

class LocationListViewModel (application: Application):AndroidViewModel(application)
{
    private var repository:LocationRepo= LocationRepo(application)
    fun getLocations()=repository.getLocations()


}