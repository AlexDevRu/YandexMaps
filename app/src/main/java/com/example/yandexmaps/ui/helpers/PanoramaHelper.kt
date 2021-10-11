package com.example.yandexmaps.ui.helpers

import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.places.PlacesFactory
import com.yandex.mapkit.places.panorama.PanoramaService
import com.yandex.runtime.Error

class PanoramaHelper {

    private lateinit var searchSession: PanoramaService.SearchSession
    private var panoramaService = PlacesFactory.getInstance().createPanoramaService()

    private lateinit var panoramaListener: PanoramaService.SearchListener

    fun findNearest(point: Point, listener: PanoramaService.SearchListener) {
        panoramaListener = listener
        searchSession = panoramaService.findNearest(point, panoramaListener)
    }
}