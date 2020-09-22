package com.my.servicetesting

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.my.servicetesting.db.LocationDao
import com.my.servicetesting.db.LocationData
import com.my.servicetesting.db.LocationDataBase
import com.my.servicetesting.util.Util
import com.my.servicetesting.util.Util.getLocationText


class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        // Used in checking for runtime permissions.
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    }


    // The BroadcastReceiver used to listen from broadcasts from the service.
    private var myReceiver: MyReceiver? = null

    // A reference to the service used to get location updates.
    private var mServiceForeground: ForegroundLocationService? = null

    // Tracks the bound state of the service.
    private var mBound = false


    // Monitors the state of the connection to the service.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundLocationService.LocalBinder
            mServiceForeground = binder.serviceForeground
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mServiceForeground = null
            mBound = false
        }
    }


    lateinit var mActivity: Activity

    private var db: LocationDataBase? = null
    private var locationDao: LocationDao? = null

    private var locationlistViewModel: LocationListViewModel? = null

    lateinit var adapter: LocationListAdapter
    lateinit var recyclerView: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        myReceiver = MyReceiver()
        mActivity = this@MainActivity
        locationlistViewModel = ViewModelProviders.of(this).get(LocationListViewModel::class.java)
        locationlistViewModel?.getLocations()?.observe(this, Observer { this.renderMessges(it) })
        locationDao = db?.LocationDao()

        if (!Util.isLocationEnabledOrNot(mActivity)) {
            Util.showAlertLocation(
                mActivity,
                getString(R.string.gps_enable),
                getString(R.string.please_turn_on_gps),
                getString(
                    R.string.ok
                )
            )
        }
        requestPermissionsSafely(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 200
        )

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            if (!checkPermissions()) {
                requestPermissions()
            } else {
                if (!Util.isLocationEnabledOrNot(mActivity)) {
                    Util.showAlertLocation(
                        mActivity,
                        getString(R.string.gps_enable),
                        getString(R.string.please_turn_on_gps),
                        getString(
                            R.string.ok
                        )
                    )
                } else
                    mServiceForeground!!.requestLocationUpdates()

            }
            findViewById<FloatingActionButton>(R.id.fab_cancel).setOnClickListener { view ->
                mServiceForeground!!.removeLocationUpdates()
            }
        }

        recyclerView = findViewById(R.id.recyclerview)
    }


    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, ForegroundLocationService::class.java), mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            myReceiver!!,
            IntentFilter(ForegroundLocationService.ACTION_BROADCAST)
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver!!)
        super.onPause()
    }

    override fun onStop() {
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
        super.onStop()
    }


    /**
     * Returns the current state of the permissions needed.
     */
    private fun checkPermissions(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            Snackbar.make(
                findViewById(R.id.fab),
                R.string.permission_rationale,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.ok) { // Request permission
                    ActivityCompat.requestPermissions(
                        this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.e(TAG, "Requesting permission")
            ActivityCompat.requestPermissions(
                this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.e(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.size <= 0) {
                Log.e(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mServiceForeground!!.requestLocationUpdates()
            } else {
                // Permission denied.

                Snackbar.make(
                    findViewById(R.id.fab),
                    R.string.permission_denied_explanation,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.settings) { // Build intent that displays the App settings screen.
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID, null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                    .show()
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    fun requestPermissionsSafely(
        permissions: Array<String>,
        requestCode: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions!!, requestCode)
        }
    }

    private inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val location =
                intent.getParcelableExtra<Location>(ForegroundLocationService.EXTRA_LOCATION)
            //can get realtime location data from here also. when the activity is live
            if (location != null) {
                Toast.makeText(
                    this@MainActivity, getLocationText(location),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun renderMessges(messages: List<LocationData>?) {
        Log.e("Size ",""+messages?.size)
        adapter = LocationListAdapter(this, messages)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }


}