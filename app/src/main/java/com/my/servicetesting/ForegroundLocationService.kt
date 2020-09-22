/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.my.servicetesting

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.my.servicetesting.db.LocationData
import com.my.servicetesting.db.LocationRepo
import com.my.servicetesting.util.Util.getLocationText
import com.my.servicetesting.util.Util.getLocationTitle
import com.my.servicetesting.util.Util.requestingLocationUpdates
import com.my.servicetesting.util.Util.setRequestingLocationUpdates
import java.text.SimpleDateFormat
import java.util.*

class ForegroundLocationService : Service() {

    companion object {
        private const val PACKAGE_NAME =
            "com.google.android.gms.location.sample.locationupdatesforegroundservice"
        private val TAG = ForegroundLocationService::class.java.simpleName
        private const val CHANNEL_ID = "channel_01"
        const val ACTION_BROADCAST = "$PACKAGE_NAME.broadcast"
        const val EXTRA_LOCATION = "$PACKAGE_NAME.location"
        private const val EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
                ".started_from_notification"

        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2
        private const val NOTIFICATION_ID = 12345678
    }

    private val mBinder: IBinder = LocalBinder()
    private var mChangingConfiguration = false
    private var mNotificationManager: NotificationManager? = null
    private var mLocationRequest: LocationRequest? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    private var mLocationCallback: LocationCallback? = null
    private var mServiceHandler: Handler? = null

    private var mLocation: Location? = null
    var mCounter = 0;

    override fun onCreate() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult.lastLocation)
            }
        }


        createLocationRequest()
        getLastLocation()
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        mServiceHandler = Handler(handlerThread.looper)
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.app_name)
            val mChannel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            mNotificationManager!!.createNotificationChannel(mChannel)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.e(TAG, "Service started")
        val startedFromNotification = intent.getBooleanExtra(
            EXTRA_STARTED_FROM_NOTIFICATION,
            false
        )
        if (startedFromNotification) {
            removeLocationUpdates()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mChangingConfiguration = true
    }

    override fun onBind(intent: Intent): IBinder? {

        Log.e(TAG, "in onBind()")
        stopForeground(true)
        mChangingConfiguration = false
        return mBinder
    }

    override fun onRebind(intent: Intent) {
        Log.e(TAG, "in onRebind()")
        stopForeground(true)
        mChangingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.e(TAG, "Last client unbound from service")

        if (!mChangingConfiguration && requestingLocationUpdates(this)) {
            Log.e(TAG, "Starting foreground service")
            startForeground(NOTIFICATION_ID, notification)
        }
        return true
    }

    override fun onDestroy() {
        mServiceHandler!!.removeCallbacksAndMessages(null)
    }


    fun requestLocationUpdates() {
        Log.e(TAG, "Requesting location updates")
        setRequestingLocationUpdates(this, true)
        startService(Intent(applicationContext, ForegroundLocationService::class.java))
        try {
            mFusedLocationClient!!.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback, Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
            setRequestingLocationUpdates(this, false)
            Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
        }
    }

    fun removeLocationUpdates() {
        Log.e(TAG, "Removing location updates")
        try {
            mFusedLocationClient!!.removeLocationUpdates(mLocationCallback)
            setRequestingLocationUpdates(this, false)
            stopSelf()
        } catch (unlikely: SecurityException) {
            setRequestingLocationUpdates(this, true)
            Log.e(TAG, "Lost location permission. Could not remove updates. $unlikely")
        }
    }


    private val notification: Notification
        private get() {
            val intent = Intent(this, ForegroundLocationService::class.java)
            var text: String = getLocationText(mLocation)!!
            text = text + " " + mCounter


            intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)
            val servicePendingIntent = PendingIntent.getService(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            //For calling MainActivity From Notification
            val activityPendingIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java), 0
            )
            val builder = NotificationCompat.Builder(this)
                .addAction(
                    R.drawable.ic_launch, getString(R.string.launch_activity),
                    activityPendingIntent
                )
                .addAction(
                    R.drawable.ic_cancel, getString(R.string.remove_location_updates),
                    servicePendingIntent
                )
                .setContentText(text)
                .setContentTitle(getLocationTitle(this))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_sample_service)
                .setTicker(text)
                .setSound(null)
                .setWhen(System.currentTimeMillis())


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL_ID) // Channel ID
            }
            return builder.build()
        }

    private fun getLastLocation() {
        try {
            mFusedLocationClient!!.lastLocation
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        mLocation = task.result
                    } else {
                        Log.e(
                            TAG,
                            "Failed to get location."
                        )
                    }
                }
        } catch (unlikely: SecurityException) {
            Log.e(
                TAG,
                "Lost location permission.$unlikely"
            )
        }
    }

    private fun onNewLocation(location: Location) {
        //   Log.e(TAG, "New location: $location")
        mLocation = location
        var repository = LocationRepo(application)
        val t = Thread(Runnable {
            Log.e("Current Size ", "" + repository.getNumFiles())
            if (repository.getNumFiles() != null)
                mCounter = 1 + repository.getNumFiles()!!
            else
                mCounter = 0
            val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
            val currentDate = sdf.format(Date())
            //  Log.e("Current Battery", "" + getBatteryPercentage(this))
            var locationData = LocationData(
                mCounter,
                getLocationText(mLocation)!!,
                currentDate,
                "" + getBatteryPercentage(this)
            )


            repository.setLocations(locationData)
            val intent = Intent(ACTION_BROADCAST)
            intent.putExtra(EXTRA_LOCATION, location)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

            // Update notification content if running as a foreground service.
            if (serviceIsRunningInForeground(this)) {
                mNotificationManager!!.notify(NOTIFICATION_ID, notification)
            }
        })
        t.setPriority(10)
        t.start()
        t.join()


    }


    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }


    inner class LocalBinder : Binder() {
        val serviceForeground: ForegroundLocationService
            get() = this@ForegroundLocationService
    }


    private fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (javaClass.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }

    private fun getBatteryPercentage(context: Context): Int {
        return if (Build.VERSION.SDK_INT >= 21) {
            val bm =
                context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, iFilter)
            val level = batteryStatus?.getIntExtra(
                BatteryManager.EXTRA_LEVEL,
                -1
            ) ?: -1
            val scale = batteryStatus?.getIntExtra(
                BatteryManager.EXTRA_SCALE,
                -1
            ) ?: -1
            val batteryPct = level / scale.toDouble()
            (batteryPct * 100).toInt()
        }
    }

}