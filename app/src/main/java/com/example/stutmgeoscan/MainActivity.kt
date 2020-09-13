package com.example.stutmgeoscan

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    //Geofence
    private lateinit var geofencingClient: GeofencingClient//cliente de geovallado

    //Receptor de emisión para las transiciones de geovallado
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    //Scan WiFi
    private lateinit var wifiManager: WifiManager

    private val wifiScanReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                scanSuccess()
            } else {
                scanFailure()
            }
        }
    }

    private val runningQOrLater =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (checkPermissions()) {
            scanReport.text = "Check permissions"
            createNotificationChannel()
            createLocationRequestAndcheckSettings()
            scanBtn.setOnClickListener { scanWifiNetworks() }

        }
    }

    //request permission
    private fun checkPermissions(): Boolean {

        if (runningQOrLater) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                21
            )
            val permissionAccessFineLocationApproved = ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
            val backgroundLocationPermissionApproved = ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

            return permissionAccessFineLocationApproved && backgroundLocationPermissionApproved

        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                22
            )

            return ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }


    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "GeofenceChannel"
            val name = "STUTM"
            val descriptionText = "Channel description"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun createLocationRequestAndcheckSettings() {

        locationRequest = LocationRequest.create()?.apply {
            //interval = 10000//revisar
            //fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }!!

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { // locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
            Log.i(tag, "Success check settings")
            addGeofences()

        }

        task.addOnFailureListener { exception ->

            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this@MainActivity,
                        29
                    )//REQUEST_CHECK_SETTINGS


                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                    //sendEx.printStackTrace()
                    Log.e(tag, "Error getting location settings resolution: " + sendEx.message)
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun addGeofences() {

        //
        geofencingClient.addGeofences(createGeofence(), geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences added
                //statusText.text = "success2"
                Log.i(tag, "Adding geofences")
            }
            addOnFailureListener {
                // Failed to add geofences

                Log.e(tag, "Fail adding geofences")
            }
        }
    }

    //Crear georefencia
    private fun createGeofence(): GeofencingRequest? {

        val geofenceList: ArrayList<Geofence> = arrayListOf()

        for (count in GeofencingConstants.Station_TM) {//posible error

            //val constants = GeofencingConstants.LANDMARK_DATA[i]
            Log.i(tag, "Add geofences: ${count.key}")

            geofenceList.add(
                Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(count.key)

                    // Set the circular region of this geofence.
                    .setCircularRegion(
                        count.Latitude,
                        count.Longitude,
                        GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
                    )

                    // Set the expiration duration of the geofence. This geofence gets automatically
                    // removed after this period of time.
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)

                    .setLoiteringDelay(GeofencingConstants.GEOFENCE_DWELL_TIME)

                    // Set the transition types of interest. Alerts are only generated for these
                    // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)

                    // Create the geofence.
                    .build()
            )
        }
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
            addGeofences(geofenceList)
        }.build()
    }


    override fun onDestroy() {
        super.onDestroy()
        removeGeofences()
    }

    private fun removeGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences removed
                Log.i(tag, "Removing geofences")
            }
            addOnFailureListener {
                // Failed to remove geofences
                Log.e(tag, "Fail removing geofences")
            }
        }
    }

    /* Scan WiFi*/


    private fun scanWifiNetworks() {
//https://github.com/shmulman/WifiSense_v5/blob/master/app/src/main/java/il/co/shmulman/www/wifisense_v5/MainActivity.kt
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)

        val success = wifiManager.startScan()
        if (!success) {
            // scan failure handling

            scanFailure()
        }
    }

    private fun scanFailure() {
        scanReport.text = ""
        scanReport.append("Scan WiFi fail")

    }

    private fun scanSuccess() {
        val results = wifiManager.scanResults
        scanReport.text = "Scan WiFi success" + "\n"
        scanReport.append("Number of access point: ")
        scanReport.append(results.size.toString() + "\n")
        //... use new scan results ...
        /*for (result in results) {
            //Toast.makeText(applicationContext, result.SSID, Toast.LENGTH_SHORT).show()
            textView.append(result.SSID + " " + result.level + " dBm " + " " + result.BSSID + "\n")

        }*/
    }
}