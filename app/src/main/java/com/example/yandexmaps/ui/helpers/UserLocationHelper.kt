package com.example.yandexmaps.ui.helpers

import android.graphics.PointF
import com.example.yandexmaps.R
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.RotationType
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider

class UserLocationHelper(private val mapView: MapView, onObjectAdded: () -> Boolean) {

    private val userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow)

    private val objectListener = object: UserLocationObjectListener {
        override fun onObjectAdded(userLocationView: UserLocationView) {

            if(onObjectAdded()) return

            val width = mapView.width()
            val height = mapView.height()

            userLocationLayer.setAnchor(
                PointF((width * 0.5).toFloat(), (height * 0.5).toFloat()),
                PointF((width * 0.5).toFloat(), (height * 0.83).toFloat())
            )

            userLocationView.arrow.setIcon(
                ImageProvider.fromResource(
                    mapView.context, R.drawable.user_arrow
                )
            )

            val pinIcon = userLocationView.pin.useCompositeIcon()

            pinIcon.setIcon(
                "icon",
                ImageProvider.fromResource(mapView.context, R.drawable.icon),
                IconStyle().setAnchor(PointF(0f, 0f))
                    .setRotationType(RotationType.ROTATE)
                    .setZIndex(0f)
                    .setScale(1f)
            )

            pinIcon.setIcon(
                "pin",
                ImageProvider.fromResource(mapView.context, R.drawable.search_result),
                IconStyle().setAnchor(PointF(0.5f, 0.5f))
                    .setRotationType(RotationType.ROTATE)
                    .setZIndex(1f)
                    .setScale(0.5f)
            )
        }

        override fun onObjectRemoved(p0: UserLocationView) {}

        override fun onObjectUpdated(p0: UserLocationView, p1: ObjectEvent) {}
    }

    init {
        userLocationLayer.isVisible = true
        userLocationLayer.isHeadingEnabled = true
        userLocationLayer.isAutoZoomEnabled = true

        userLocationLayer.setObjectListener(objectListener)
    }

    fun resetAnchor() {
        userLocationLayer.resetAnchor()
    }

    val userLocation: CameraPosition?
        get() = userLocationLayer.cameraPosition()
}