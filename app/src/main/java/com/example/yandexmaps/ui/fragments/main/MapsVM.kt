package com.example.yandexmaps.ui.fragments.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.yandexmaps.ui.models.SearchResponseModel
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition

class MapsVM: ViewModel() {

    val query = MutableLiveData<String>()

    var userLocation = Point()

    val selectedGeoObject = MutableLiveData<GeoObject?>(null)

    val suggestionsList = MutableLiveData<List<String?>>()

    val searchResponse = MutableLiveData<SearchResponseModel>(null)

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

    var userAdded = false

    var markerMode = MARKER_MODE.PLACE

    var cameraPosition: CameraPosition? = null
}

enum class MARKER_MODE {
    PLACE, ORIGIN, DESTINATION
}