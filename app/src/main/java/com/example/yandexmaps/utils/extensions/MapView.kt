package com.example.yandexmaps.utils.extensions

import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.mapview.MapView

fun MapView.visibleRegion() = VisibleRegionUtils.toPolygon(map.visibleRegion)

fun MapView.moveCameraToBoundingBox(boundingBox: BoundingBox, zoomIndent: Float = 0.8f) {
    var cameraPosition = map.cameraPosition(boundingBox)
    cameraPosition = CameraPosition(cameraPosition.target, cameraPosition.zoom - zoomIndent, cameraPosition.azimuth, cameraPosition.tilt)
    map.move(cameraPosition, Animation(Animation.Type.SMOOTH, 1f), null)
}
