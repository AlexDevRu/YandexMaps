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
            //binding.panoramaButton.visibility = View.VISIBLE
        }

        override fun onPanoramaSearchError(error: Error) {
            onFailure(error)
            //binding.panoramaButton.visibility = View.GONE
        }
    }

    fun findNearest(point: Point, onSuccess: (String) -> Unit, onError: (Error) -> Unit) {
        this.onSuccess = onSuccess
        this.onFailure = onFailure
        panoramaService.findNearest(point, panoramaListener)
    }
}