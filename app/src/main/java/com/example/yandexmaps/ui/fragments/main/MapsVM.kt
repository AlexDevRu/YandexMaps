package com.example.yandexmaps.ui.fragments.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.yandexmaps.ui.models.SearchResponseModel
import com.example.yandexmaps.utils.SingleLiveEvent
import com.example.yandexmaps.utils.extensions.DirectionVM
import com.example.yandexmaps.utils.extensions.SearchVM
import com.example.yandexmaps.utils.extensions.UserLocationVM
import com.example.yandexmaps.utils.extensions.uri
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.search.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class AddressNotFind: Exception()


sealed class Result<T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure<T>(val throwable: Throwable) : Result<T>()
    class Loading<T>: Result<T>()
}

class MapsVM(): ViewModel() {

    companion object {
        private const val TAG = "MapsVM"
    }

    private val _cameraPosition = MutableLiveData<CameraPosition?>(null)
    val cameraPosition: LiveData<CameraPosition?> = _cameraPosition

    private val _origin = MutableStateFlow<Point?>(null)
    val origin: StateFlow<Point?> = _origin

    private val _destination = MutableLiveData<Point?>(null)
    val destination: LiveData<Point?> = _destination

    private val _originAddress = MutableLiveData<Result<String?>>(null)
    var originAddress: LiveData<Result<String?>> = _originAddress

    private val _destinationAddress = MutableLiveData<Result<String?>>(null)
    var destinationAddress: LiveData<Result<String?>> = _destinationAddress

    val directionAction = SingleLiveEvent(DIRECTION_ACTION.BIND_MY_LOCATION)

    private val _selectedGeoObject = MutableLiveData<GeoObject?>()
    val selectedGeoObject: LiveData<GeoObject?> = _selectedGeoObject

    private val _selectedGeoObjectMetadata = MutableLiveData<Result<GeoObjectMetadataModel>?>()
    val selectedGeoObjectMetadata: LiveData<Result<GeoObjectMetadataModel>?> = _selectedGeoObjectMetadata

    private val _markerMode = MutableLiveData(MARKER_MODE.PLACE)
    val markerMode: LiveData<MARKER_MODE> = _markerMode


    private val _directionMarkerType = MutableLiveData(DIRECTION_MARKER_TYPE.DESTINATION)
    val directionMarkerType: LiveData<DIRECTION_MARKER_TYPE> = _directionMarkerType

    private val _directionType = MutableLiveData(DIRECTION_TYPE.DRIVING)
    val directionType: LiveData<DIRECTION_TYPE> = _directionType




    val directionVM = DirectionVM()
    val userLocationVM = UserLocationVM(::syncDirectionPoint)
    val searchVM = SearchVM()

    fun setMarker(geoObject: GeoObject) {
        when(markerMode.value) {
            MARKER_MODE.PLACE -> {
                _selectedGeoObject.value = geoObject
                val uri = geoObject.uri()
                if(uri != null) {
                    searchVM.searchByUri(uri, {
                        _selectedGeoObjectMetadata.value = Result.Success(it)
                    }, {
                        _selectedGeoObjectMetadata.value = Result.Loading()
                    }, {
                        _selectedGeoObjectMetadata.value = Result.Failure(it)
                    })
                }
            }
            MARKER_MODE.DIRECTION -> {
                when(directionMarkerType.value) {
                    DIRECTION_MARKER_TYPE.ORIGIN -> {
                        setOrigin(geoObject.geometry.firstOrNull()?.point)
                    }
                    DIRECTION_MARKER_TYPE.DESTINATION -> {
                        setDestination(geoObject.geometry.firstOrNull()?.point)
                    }
                }
            }
        }
    }
    fun setMarker(point: Point?) {
        when(markerMode.value) {
            MARKER_MODE.PLACE -> {
                _selectedGeoObject.value = null
            }
            MARKER_MODE.DIRECTION -> {
                when(directionMarkerType.value) {
                    DIRECTION_MARKER_TYPE.ORIGIN -> {
                        setOrigin(point)
                    }
                    DIRECTION_MARKER_TYPE.DESTINATION -> {
                        setDestination(point)
                    }
                }
            }
        }
    }

    fun updateCameraPosition(newCameraPosition: CameraPosition) {
        _cameraPosition.value = newCameraPosition
    }

    fun applyDirectionAction(action: DIRECTION_ACTION, directionMarkerType: DIRECTION_MARKER_TYPE) {
        when(action) {
            DIRECTION_ACTION.BIND_MY_LOCATION -> {
                directionMarkerType.binded = true
            }
            DIRECTION_ACTION.CHOOSE_ON_MAP -> {
                directionMarkerType.binded = false
            }
        }
        directionAction.value = action
        syncDirectionPoint()
    }
    fun applyDirectionAction(action: DIRECTION_ACTION) {
        when(action) {
            DIRECTION_ACTION.BIND_MY_LOCATION -> {
                directionMarkerType.value?.binded = true
                syncDirectionPoint()
            }
            DIRECTION_ACTION.CHOOSE_ON_MAP -> {
                directionMarkerType.value?.binded = false
            }
        }
        directionAction.value = action
    }

    private fun syncDirectionPoint() {
        Log.w(TAG, "dfjgkjfgljdfgjdflgj ${DIRECTION_MARKER_TYPE.ORIGIN.binded}")
        if(directionAction.value == DIRECTION_ACTION.BIND_MY_LOCATION) {
            if(DIRECTION_MARKER_TYPE.ORIGIN.binded) {
                setOrigin(userLocationVM.userLocation.value)
            } else if(DIRECTION_MARKER_TYPE.DESTINATION.binded) {
                setDestination(userLocationVM.userLocation.value)
            }
        }
    }

    fun setOrigin(point: Point?) {
        _origin.value = point
        val zoom = cameraPosition.value?.zoom?.toInt() ?: 11
        searchVM.searchByPoint(point, zoom, {
            _originAddress.value = Result.Success(it.collection.children.firstOrNull()?.obj?.name)
        }, {
            _originAddress.value = Result.Loading()
        }, {
            _originAddress.value = Result.Failure(it)
        })

        buildDirection()
    }
    fun setDestination(point: Point?) {
        _destination.value = point
        val zoom = cameraPosition.value?.zoom?.toInt() ?: 11
        searchVM.searchByPoint(point, zoom, {
            _destinationAddress.value = Result.Success(it.collection.children.firstOrNull()?.obj?.name)
        }, {
            _destinationAddress.value = Result.Loading()
        }, {
            _destinationAddress.value = Result.Failure(it)
        })
        buildDirection()
    }

    fun setDirectionType(newDirectionType: DIRECTION_TYPE) {
        _directionType.value = newDirectionType
        buildDirection()
    }

    fun updateDirectionMarkerType(newDirectionMarkerType: DIRECTION_MARKER_TYPE) {
        _directionMarkerType.value = newDirectionMarkerType
    }

    fun toggleMarkerMode() {
        _markerMode.value = markerMode.value?.toggle()
    }


    private fun getAddressBySearchResponse(resultResponse: Result<Response>) = when (resultResponse) {
        is Result.Success -> Result.Success(resultResponse.value.metadata.toponym?.name)
        is Result.Failure -> Result.Failure(resultResponse.throwable)
        is Result.Loading -> Result.Loading()
    }


    private fun buildDirection() {
        val originPoint = origin.value
        val destinationPoint = destination.value

        if(markerMode.value == MARKER_MODE.DIRECTION && originPoint != null && destinationPoint != null) {
            Log.e("asd", "reload direction")

            when(directionType.value) {
                DIRECTION_TYPE.DRIVING -> {
                    directionVM.submitDrivingRequest(originPoint, destinationPoint)
                }
                DIRECTION_TYPE.MASS_TRANSIT -> {
                    directionVM.submitMassTransitRequest(originPoint, destinationPoint)
                }
            }
        }
    }
}


/*class MapsVM: ViewModel() {

    companion object {
        private const val TAG = "MapsVM"
    }

    var userLocation = Point()
    private var lastKnownUserLocation: Point? = null

    val selectedGeoObject = MutableLiveData<GeoObject?>(null)

    val searchResponse = MutableLiveData<SearchResponseModel?>(null)
    val searchLayerQuery = SingleLiveEvent<String?>(null)

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
    val originAddress = MutableLiveData<String?>(null)
    val destinationAddress = MutableLiveData<String?>(null)

    val selectedObjectMetaDatas = MutableLiveData<GeoObjectMetadataModel?>(null)

    val drivingRoutes = MutableLiveData<List<DrivingRoute>?>()
    val massTransitRoutes = MutableLiveData<List<Route>?>()

    var userAdded = false

    val markerMode = MutableLiveData(MARKER_MODE.PLACE)
    val directionMarkerType = MutableLiveData(DIRECTION_MARKER_TYPE.DESTINATION)

    var cameraPosition: CameraPosition? = null


    var directionShouldBeReload = false


    val locationUpdateListener = object : LocationListener {
        override fun onLocationStatusUpdated(p0: LocationStatus) {
            Log.w(TAG, p0.toString())
        }
        override fun onLocationUpdated(location: Location) {
            val lat = location.position.latitude
            val lng = location.position.longitude

            Log.w(TAG, "lat=${lat} " + "lon=${lng}")
            lastKnownUserLocation = Point(userLocation.latitude, userLocation.longitude)
            userLocation = location.position

            directionShouldBeReload = lat != lastKnownUserLocation!!.latitude || lng != lastKnownUserLocation!!.longitude

            //syncDirectionPoint()
        }
    }

    private fun syncDirectionPoint() {
        if(directionAction.value == DIRECTION_ACTION.BIND_MY_LOCATION) {
            directionShouldBeReload = true
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

    var directionType = MutableLiveData(DIRECTION_TYPE.DRIVING)



    private val searchManager = SearchFactory.getInstance().createSearchManager(
        SearchManagerType.COMBINED)

    private lateinit var searchSession: Session


    private fun getSearchListener(onResponse: (Response) -> Unit, onFailure: (Error) -> Unit) = object: Session.SearchListener {
        override fun onSearchResponse(response: Response) {
            onResponse(response)
        }

        override fun onSearchError(error: Error) {
            onFailure(error)
        }
    }

    private lateinit var searchByPointListener: Session.SearchListener
    private lateinit var searchByQueryListener: Session.SearchListener


    fun searchByPoint(point: Point, onResponse: (Response) -> Unit, onFailure: (Error) -> Unit) {
        searchByPointListener = getSearchListener(onResponse, onFailure)
        searchSession = searchManager.submit(
            point,
            cameraPosition?.zoom?.toInt(),
            SearchOptions(),
            searchByPointListener
        )
    }

    fun searchByUri(uri: String, onResponse: (Response) -> Unit, onFailure: (Error) -> Unit) {
        searchSession = searchManager.searchByURI(
            uri,
            SearchOptions().setGeometry(true).setSnippets(
                Snippet.PANORAMAS.value or Snippet.BUSINESS_IMAGES.value or
                        Snippet.BUSINESS_RATING1X.value or
                        Snippet.PHOTOS.value
            ),
            object: Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    onResponse(response)
                }

                override fun onSearchError(error: Error) {
                    onFailure(error)
                }
            }
        )
    }

    fun submitQuery(query: String, geometry: Geometry, listener: Session.SearchListener) {
        searchByQueryListener = listener
        searchSession = searchManager.submit(
            query,
            geometry,
            SearchOptions(),
            searchByQueryListener
        )
    }


    fun setDirectionMarkerByPoint(point: Point) {
        directionShouldBeReload = true
        if(directionMarkerType.value == DIRECTION_MARKER_TYPE.ORIGIN) {
            origin.value = point
        } else if(directionMarkerType.value == DIRECTION_MARKER_TYPE.DESTINATION) {
            destination.value = point
        }
    }
}*/

enum class MARKER_MODE {
    PLACE {
        override fun toggle() = DIRECTION
    },
    DIRECTION {
        override fun toggle() = PLACE
    };

    abstract fun toggle(): MARKER_MODE
}

enum class DIRECTION_MARKER_TYPE(var binded: Boolean = false) {
    ORIGIN(true), DESTINATION
}

enum class DIRECTION_ACTION {
    BIND_MY_LOCATION, CHOOSE_ON_MAP
}

enum class DIRECTION_TYPE {
    DRIVING, MASS_TRANSIT
}


data class GeoObjectMetadataModel(
    val response: Response?,
    val geoObject: GeoObject?,
    val panoramasMetadata: PanoramasObjectMetadata?,
    val imagesMetadata: BusinessImagesObjectMetadata?,
    val photosMetadata: BusinessPhotoObjectMetadata?,
    val generalMetadata: BusinessObjectMetadata,
)