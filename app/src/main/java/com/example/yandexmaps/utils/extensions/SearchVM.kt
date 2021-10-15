package com.example.yandexmaps.utils.extensions

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.yandexmaps.ui.fragments.main.AddressNotFind
import com.example.yandexmaps.ui.fragments.main.GeoObjectMetadataModel
import com.example.yandexmaps.ui.fragments.main.Result
import com.example.yandexmaps.ui.models.SearchResponseModel
import com.example.yandexmaps.utils.Utils
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.*
import com.yandex.runtime.Error



class SearchVM {

    companion object {
        private const val TAG = "SearchVM"
    }

    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)

    private var searchSession: Session? = null

    private val _searchResponse = MutableLiveData<Result<SearchResponseModel?>>()
    val searchResponse: LiveData<Result<SearchResponseModel?>> = _searchResponse

    fun searchByPoint(point: Point?, zoom: Int,
                      onSuccess: (Response) -> Unit,
                      onLoading: () -> Unit,
                      onFailure: (Exception) -> Unit
    ): LiveData<Result<Response>> {

        searchSession?.cancel()
        val searchResponse = MutableLiveData<Result<Response>>()

        if(point == null) {
            searchResponse.value = Result.Failure(AddressNotFind())
            onFailure(AddressNotFind())
        } else {
            searchResponse.value = Result.Loading()
            onLoading()

            searchSession = searchManager.submit(
                point,
                zoom,
                SearchOptions(),
                object: Session.SearchListener {
                    override fun onSearchResponse(response: Response) {
                        searchResponse.value = Result.Success(response)
                        onSuccess(response)
                    }

                    override fun onSearchError(error: Error) {
                        val errorMessage = Utils.getErrorMessage(error)
                        searchResponse.value = Result.Failure(Exception(errorMessage.toString()))
                        onFailure(Exception(errorMessage.toString()))
                    }
                }
            )
        }

        return searchResponse
    }

    fun searchByUri(
        uri: String?,
        onSuccess: (GeoObjectMetadataModel) -> Unit,
        onLoading: () -> Unit,
        onFailure: (Exception) -> Unit
    ): LiveData<Result<GeoObjectMetadataModel>> {

        searchSession?.cancel()
        val searchResponse = MutableLiveData<Result<GeoObjectMetadataModel>>()

        if(uri == null) {
            searchResponse.value = Result.Failure(AddressNotFind())
            onFailure(AddressNotFind())
        } else {
            onLoading()
            searchSession = searchManager.searchByURI(
                uri,
                SearchOptions().setGeometry(true).setSnippets(
                    Snippet.PANORAMAS.value or Snippet.BUSINESS_IMAGES.value or
                            Snippet.BUSINESS_RATING1X.value or
                            Snippet.PHOTOS.value
                ),
                object: Session.SearchListener {
                    override fun onSearchResponse(response: Response) {
                        Log.d(TAG, "collection ${response.collection.children.first().obj}")

                        val obj = response.collection.children.first().obj
                        val metadata = obj?.metadataContainer?.getItem(BusinessObjectMetadata::class.java)
                        val panoramasMetadata = obj?.metadataContainer?.getItem(PanoramasObjectMetadata::class.java)
                        val imagesMetadata = obj?.metadataContainer?.getItem(BusinessImagesObjectMetadata::class.java)
                        val photosMetadata = obj?.metadataContainer?.getItem(BusinessPhotoObjectMetadata::class.java)
                        val ratingMetadata = obj?.metadataContainer?.getItem(BusinessRating1xObjectMetadata::class.java)
                        val fuelMetadata = obj?.metadataContainer?.getItem(FuelMetadata::class.java)

                        val model = GeoObjectMetadataModel(
                            response,
                            obj,
                            panoramasMetadata,
                            imagesMetadata,
                            photosMetadata,
                            metadata!!
                        )
                        searchResponse.value = Result.Success(model)
                        onSuccess(model)
                    }

                    override fun onSearchError(error: Error) {
                        val errorMessage = Utils.getErrorMessage(error)
                        searchResponse.value = Result.Failure(Exception(errorMessage.toString()))
                    }
                }
            )
        }

        return searchResponse
    }

    fun clearSearch() {
        _searchResponse.value = Result.Success(null)
    }
}