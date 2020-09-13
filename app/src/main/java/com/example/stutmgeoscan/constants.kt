package com.example.stutmgeoscan

data class StationDataObject(val key: String, val Latitude: Double, val Longitude: Double)

internal object GeofencingConstants {


    val Station_TM = arrayOf(

        StationDataObject(
            "Calle 45",
            5.7541223,
            -75.0985158),

        StationDataObject(
            "Calle 22",
            4.8541223,
            -76.0985158),

        StationDataObject(
            "Casa",
            4.7541223,
            -74.0985158),

        StationDataObject(
            "casa2",
            4.7341223,
            -73.0985158)


    )
    const val GEOFENCE_DWELL_TIME = 30000
    const val GEOFENCE_RADIUS_IN_METERS = 50f
}