package com.example.stutmgeoscan

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent


class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val tag = "GeofenceBroadcastReceiver"
    private val mainActivity = MainActivity.instance

    private val channelId  = "GeofenceChannel"
    private val notificationId = 420

    override fun onReceive(context: Context?, intent: Intent?) {

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            //val errorMessage = GeofenceStatusCodes.getErrorString(geofencingEvent.errorCode)
            Log.e(tag, "errorMessage")
            return
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Get the geofences that were triggered. A single event can trigger
        // multiple geofences.
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        // Get the transition details as a String.
        val geofenceTransitionDetails = getGeofenceTransitionDetails(
            geofenceTransition,
            triggeringGeofences
        )

        // Send notification and log the transition details.
        if (context != null) {
            sendNotification(geofenceTransitionDetails, context)
        }

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL ) {

            mainActivity.scanWifiNetworks()

        }else{
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){

            }
        }
    }

    private fun getGeofenceTransitionDetails(
        geofenceTransition: Int,
        triggeringGeofences: MutableList<Geofence>
    ): String {

        val geofenceTransitionString = getTransitionString(geofenceTransition)

        // Get the Ids of each geofence that was triggered.
        val triggeringGeofencesIdsList: ArrayList<String> = arrayListOf()
        for (geofence in triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.requestId)
        }
        val triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList)

        return "$geofenceTransitionString: $triggeringGeofencesIdsString"
    }

    private fun getTransitionString(transitionType: Int): String {
        return when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "GEOFENCE_TRANSITION_ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "GEOFENCE_TRANSITION_EXIT"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "GEOFENCE_TRANSITION_DWELL"
            else -> "GEOFENCE_TRANSITION_UNKNOWN"
        }
    }

    private fun sendNotification(geofenceTransitionDetails: String, context: Context) {

        // Create an explicit intent for an Activity in your app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val builder = NotificationCompat.Builder(context, channelId)

            //https://walkiriaapps.com/blog/android/iconos-notificaciones-android-studio/
            .setSmallIcon(R.drawable.geofence_icon)
            .setContentTitle("Activación geovallado")
            .setContentText(geofenceTransitionDetails)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        Log.i(tag, "Send notification")

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }

    }

}