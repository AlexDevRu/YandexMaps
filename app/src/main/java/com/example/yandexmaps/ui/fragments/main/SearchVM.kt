package com.example.yandexmaps.ui.fragments.main

import android.util.Log
import com.example.yandexmaps.ui.models.GeoObjectMetadataModel
import com.example.yandexmaps.utils.Utils
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.*
import com.yandex.runtime.Error



class SearchVM {

    companion object {
        private const val TAG = "SearchVM"
    }

    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)

    private var querySearchSession: Session? = null
    private var pointSearchSession: Session? = null
    private var uriSearchSession: Session? = null

    fun searchByQuery(query: String?,
                      geometry: Geometry,
                      onSuccess: (Response) -> Unit,
                      onLoading: () -> Unit,
                      onFailure: (Exception) -> Unit) {

        if(query == null) onFailure(Exception())

        onLoading()
        querySearchSession?.cancel()
        querySearchSession = searchManager.submit(
            query!!,
            geometry,
            SearchOptions(),
            object: Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    onSuccess(response)
                }

                override fun onSearchError(error: Error) {
                    val errorMessage = Utils.getErrorMessage(error)
                    onFailure(Exception(errorMessage.toString()))
                }
            }
        )
    }


    fun searchByPoint(point: Point?, zoom: Int,
                      onSuccess: (Response) -> Unit,
                      onLoading: () -> Unit,
                      onFailure: (Exception) -> Unit
    ) {

        pointSearchSession?.cancel()

        if(point == null) {
            onFailure(AddressNotFind())
        } else {
            onLoading()

            pointSearchSession = searchManager.submit(
                point,
                zoom,
                SearchOptions(),
                object: Session.SearchListener {
                    override fun onSearchResponse(response: Response) {
                        onSuccess(response)
                    }

                    override fun onSearchError(error: Error) {
                        val errorMessage = Utils.getErrorMessage(error)
                        onFailure(Exception(errorMessage.toString()))
                    }
                }
            )
        }
    }

    fun searchByUri(
        uri: String?,
        onSuccess: (GeoObjectMetadataModel) -> Unit,
        onLoading: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {

        uriSearchSession?.cancel()

        if(uri == null) {
            onFailure(AddressNotFind())
        } else {
            onLoading()
            uriSearchSession = searchManager.searchByURI(
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
                        onSuccess(model)
                    }

                    override fun onSearchError(error: Error) {
                        val errorMessage = Utils.getErrorMessage(error)
                        onFailure(Exception(errorMessage.toString()))
                    }
                }
            )
        }
    }
}