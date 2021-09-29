package com.example.yandexmaps.ui.helpers

import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.places.PlacesFactory
import com.yandex.mapkit.places.panorama.PanoramaService
import com.yandex.runtime.Error

class PanoramaHelper {

    private var panoramaService = PlacesFactory.getInstance().createPanoramaService()

    private var onSuccess: (String) -> Unit = {}
    private var onFailure: (Error) -> Unit = {}

    private val panoramaListener = object: PanoramaService.SearchListener {
        override fun onPanoramaSearchResult(p0: String) {
            onSuccess(p0)
        }

        override fun onPanoramaSearchError(error: Error) {
            onFailure(error)
        }
    }

    fun findNearest(point: Point, onSuccess: (String) -> Unit, onFailure: (Error) -> Unit) {
        this.onSuccess = onSuccess
        this.onFailure = onFailure
        panoramaService.findNearest(point, panoramaListener)
    }
}