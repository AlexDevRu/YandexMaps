package com.example.yandexmaps.ui.fragments.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.yandexmaps.ui.models.SearchResponseModel
import com.example.yandexmaps.utils.SingleLiveEvent
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
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
    var directionMarkerType = MutableLiveData(DIRECTION_MARKER_TYPE.DESTINATION)

    var cameraPosition: CameraPosition? = null


    val locationUpdateListener = object : LocationListener {
        override fun onLocationStatusUpdated(p0: LocationStatus) {
            Log.w(TAG, p0.toString())
        }
        override fun onLocationUpdated(location: Location) {
            val lat = location.position.latitude
            val lng = location.position.longitude

            Log.w(TAG, "lat=${lat} " + "lon=${lng}")
            userLocation = location.position

            syncDirectionPoint()
        }
    }

    fun syncDirectionPoint() {
        if(directionAction.value == DIRECTION_ACTION.BIND_MY_LOCATION) {
            if(bindedMarkerType == DIRECTION_MARKER_TYPE.ORIGIN) {
                origin.value = userLocation
            } else if(bindedMarkerType == DIRECTION_MARKER_TYPE.DESTINATION) {
                destination.value = userLocation
            }
        }
    }


    val directionAction = SingleLiveEvent<DIRECTION_ACTION>()

    var bindedMarkerType: DIRECTION_MARKER_TYPE? = null
        private set

    fun applyDirectionAction(action: DIRECTION_ACTION, bindMarkerMode: DIRECTION_MARKER_TYPE? = null) {
        Log.w(TAG, "applyDirectionAction $action")
        if(action == DIRECTION_ACTION.BIND_MY_LOCATION) {
            bindedMarkerType = bindMarkerMode ?: directionMarkerType.value
        } else {
            bindedMarkerType = null
        }
        directionAction.value = action
        syncDirectionPoint()
    }

    init {

    }
}

enum class MARKER_MODE {
    PLACE, DIRECTION
}

enum class DIRECTION_MARKER_TYPE {
    ORIGIN, DESTINATION
}

enum class DIRECTION_ACTION {
    BIND_MY_LOCATION, CHOOSE_ON_MAP
}