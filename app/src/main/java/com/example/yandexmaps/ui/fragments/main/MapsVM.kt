package com.example.yandexmaps.ui.fragments.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.yandexmaps.ui.models.GeoObjectMetadataModel
import com.example.yandexmaps.ui.models.SearchResponseModel
import com.example.yandexmaps.utils.SingleLiveEvent
import com.example.yandexmaps.utils.extensions.DirectionVM
import com.example.yandexmaps.utils.extensions.distance
import com.example.yandexmaps.utils.extensions.uri
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.Geometry
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
        private const val minDistanceForUpdateDirection = 2.0
    }

    private val _cameraPosition = MutableLiveData<CameraPosition?>(null)
    val cameraPosition: LiveData<CameraPosition?> = _cameraPosition

    private val _origin = MutableLiveData<Point?>(null)
    val origin: LiveData<Point?> = _origin

    private val _destination = MutableLiveData<Point?>(null)
    val destination: LiveData<Point?> = _destination

    private val _originAddress = MutableLiveData<Result<String?>>(null)
    val originAddress: LiveData<Result<String?>> = _originAddress

    private val _destinationAddress = MutableLiveData<Result<String?>>(null)
    val destinationAddress: LiveData<Result<String?>> = _destinationAddress

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

    private val _searchResponse = MutableLiveData<Result<SearchResponseModel?>>()
    val searchResponse: LiveData<Result<SearchResponseModel?>> = _searchResponse


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
                _selectedGeoObjectMetadata.value = null
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

    private fun isDirectionShouldBeUpdated(point: Point?): Boolean {
        val distance = if(point != null && userLocationVM.userLocation.value != null)
            point.distance(userLocationVM.userLocation.value!!)
        else 0.0

        return distance >= minDistanceForUpdateDirection
    }

    private fun syncDirectionPoint() {
        if(directionAction.value == DIRECTION_ACTION.BIND_MY_LOCATION) {
            if(DIRECTION_MARKER_TYPE.ORIGIN.binded) {
                if(isDirectionShouldBeUpdated(origin.value)) {
                    setOrigin(userLocationVM.userLocation.value)
                }
            } else if(DIRECTION_MARKER_TYPE.DESTINATION.binded) {
                if(isDirectionShouldBeUpdated(destination.value)) {
                    setDestination(userLocationVM.userLocation.value)
                }
            }
        }
    }

    fun setOrigin(point: Point?) {
        _origin.value = point
        val zoom = cameraPosition.value?.zoom?.toInt() ?: 11
        searchVM.searchByPoint(point, zoom, {
            //val toponym = it.collection.metadataContainer.getItem(ToponymObjectMetadata::class.java)
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

    fun clearSearch() {
        _searchResponse.value = Result.Success(null)
    }

    private lateinit var visibleRegion: Geometry

    fun updateVisibleRegion(geometry: Geometry) {
        visibleRegion = geometry
    }

    fun searchByQuery(query: String?, onSuccess: () -> Unit = {},
                      onLoading: () -> Unit = {},
                      onFailure: (Exception) -> Unit = {}) {
        searchVM.searchByQuery(query, visibleRegion, {
            _searchResponse.value = Result.Success(
                SearchResponseModel(
                    it,
                    query.orEmpty(),
                    true
                )
            )
            onSuccess()
        }, {
            _searchResponse.value = Result.Loading()
            onLoading()
        }, {
            _searchResponse.value = Result.Failure(it)
            onFailure(it)
        })
    }


    fun searchBySuggestion(
        suggestion: SuggestItem,
        onSuccess: () -> Unit,
        onLoading: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.w(TAG, "suggestion uri ${suggestion.uri}")
        if(suggestion.uri != null)
            searchVM.searchByUri(suggestion.uri!!, { metadata ->

                _searchResponse.value = Result.Success(
                    SearchResponseModel(
                        metadata.response,
                        suggestion.displayText.orEmpty(),
                        false
                    )
                )

                onSuccess()
            }, {
                _searchResponse.value = Result.Loading()
                onLoading()
            }, {
                _searchResponse.value = Result.Failure(it)
                onFailure(it)
            })
        else {
            searchByQuery(suggestion.displayText, onSuccess, onLoading, onFailure)
        }
    }
}

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
