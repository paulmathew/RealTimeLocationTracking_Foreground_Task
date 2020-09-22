package com.my.servicetesting.util

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import com.my.servicetesting.R
import java.text.DateFormat
import java.util.*


object Util {
    fun isMyServiceRunning(serviceClass: Class<*>, mActivity: Activity): Boolean {
        val manager: ActivityManager =
            mActivity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.getClassName()) {
                Log.i("Service status", "Running")
                return true
            }
        }
        Log.i("Service status", "Not running")
        return false
    }

    fun isLocationEnabledOrNot(context: Context): Boolean {
        var locationManager: LocationManager? = null
        locationManager =
            context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        return locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager!!.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    fun showAlertLocation(context: Context, title: String, message: String, btnText: String) {
        val alertDialog = AlertDialog.Builder(context).create()
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setButton(btnText) { dialog, which ->
            dialog.dismiss()
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        alertDialog.show()
    }

    const val KEY_REQUESTING_LOCATION_UPDATES = "requesting_location_updates"
    fun requestingLocationUpdates(context: Context?): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(
                KEY_REQUESTING_LOCATION_UPDATES,
                false
            )
    }
    fun setRequestingLocationUpdates(
        context: Context?,
        requestingLocationUpdates: Boolean
    ) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(
                KEY_REQUESTING_LOCATION_UPDATES,
                requestingLocationUpdates
            )
            .apply()
    }

    fun getLocationText(location: Location?): String? {
        return if (location == null) "Unknown location" else "(" + location.latitude + ", " + location.longitude + ")"
    }

    fun getLocationTitle(context: Context): String? {
        return context.getString(
            R.string.location_updated,
            DateFormat.getDateTimeInstance().format(Date())
        )
    }


}