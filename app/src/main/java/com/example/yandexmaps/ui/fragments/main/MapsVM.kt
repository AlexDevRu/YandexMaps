package com.example.yandexmaps.ui.fragments.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.yandexmaps.ui.models.SearchResponseModel
import com.example.yandexmaps.utils.SingleLiveEvent
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.FilteringMode
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.CameraPosition

class MapsVM: ViewModel() {

    companion object {
        private const val TAG = "MapsVM"
    }

    val query = MutableLiveData<String>()

    var userLocation = Point()

    val selectedGeoObject = MutableLiveData<GeoObject?>(null)

    val suggestionsList = MutableLiveData<List<String?>>()

    val searchResponse = MutableLiveData<SearchResponseModel?>(null)

    private val BOX_SIZE = 0.2

    val boundingBox: BoundingBox
        get() {
            val lat = userLocation.latitude
            val lng = userLocation.longitude

            return BoundingBox(
                Point(lat - BOX_SIZE, lng - BOX_SIZE),
                Point(lat + BOX_SIZE, lng + BOX_SIZE)
            )
        }

    val origin = MutableLiveData<Point?>(null)
    val destination = MutableLiveData<Point?>(null)
    var originAddress = MutableLiveData<String?>(null)
    var destinationAddress = MutableLiveData<String?>(null)

    var userAdded = false

    var markerMode = MutableLiveData(MARKER_MODE.PLACE)

    var cameraPosition: CameraPosition? = null

    private val locationManager = MapKitFactory.getInstance().createLocationManager()


    private val locationUpdateListener = object : LocationListener {
        override fun onLocationStatusUpdated(p0: LocationStatus) {
            Log.w(TAG, p0.toString())
        }
        override fun onLocationUpdated(location: Location) {
            val lat = location.position.latitude
            val lng = location.position.longitude

            Log.w(TAG, "lat=${lat} " + "lon=${lng}")
            userLocation = location.position

            if(directionAction.value == DIRECTION_ACTION.BIND_MY_LOCATION) {
                if(bindedMarkerType == MARKER_MODE.ORIGIN) {
                    origin.value = userLocation
                } else if(bindedMarkerType == MARKER_MODE.DESTINATION) {
                    destination.value = userLocation
                }
            }
        }
    }


    init {
        locationManager.subscribeForLocationUpdates(0.0, 3000, 2.0, true, FilteringMode.ON, locationUpdateListener)
    }

    val directionAction = SingleLiveEvent<DIRECTION_ACTION>()

    private var bindedMarkerType: MARKER_MODE? = null

    fun applyDirectionAction(action: DIRECTION_ACTION) {
        Log.w(TAG, "applyDirectionAction $action")
        directionAction.value = action
        if(action == DIRECTION_ACTION.BIND_MY_LOCATION) {
            bindedMarkerType = markerMode.value
        }
    }
}

enum class MARKER_MODE {
    PLACE, ORIGIN, DESTINATION
}

enum class DIRECTION_ACTION {
    BIND_MY_LOCATION, CHOOSE_ON_MAP
}