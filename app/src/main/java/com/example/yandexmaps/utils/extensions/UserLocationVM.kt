package com.example.yandexmaps.utils.extensions

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.FilteringMode
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus

class UserLocationVM(private val onLocationUpdated: () -> Unit) {

    companion object {
        private const val TAG = "UserLocationVM"
    }

    var userLocation = MutableLiveData<Point?>()

    private val BOX_SIZE = 0.2

    val boundingBox: BoundingBox?
        get() {
            if(userLocation.value == null)
                return null

            val lat = userLocation.value!!.latitude
            val lng = userLocation.value!!.longitude

            return BoundingBox(
                Point(lat - BOX_SIZE, lng - BOX_SIZE),
                Point(lat + BOX_SIZE, lng + BOX_SIZE)
            )
        }

    private var lastKnownUserLocation: Point? = null
    private val locationManager = MapKitFactory.getInstance().createLocationManager()


    private val locationUpdateListener
        get() = object : LocationListener {
            override fun onLocationStatusUpdated(p0: LocationStatus) {
                Log.w(TAG, p0.toString())
            }
            override fun onLocationUpdated(location: Location) {
                val lat = location.position.latitude
                val lng = location.position.longitude

                Log.w(TAG, "lat=${lat} " + "lon=${lng}")
                if(userLocation.value != null) {
                    lastKnownUserLocation = Point(userLocation.value!!.latitude, userLocation.value!!.longitude)
                }
                userLocation.value = location.position

                onLocationUpdated()
            }
        }

    fun observeUserLocation() {
        locationManager.requestSingleUpdate(locationUpdateListener)
        locationManager.subscribeForLocationUpdates(0.0, 3000, 2.0, true, FilteringMode.OFF, locationUpdateListener)
    }
}