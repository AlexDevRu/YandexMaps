package com.example.yandexmaps.ui.helpers

import android.graphics.BitmapFactory
import com.example.yandexmaps.R
import com.example.yandexmaps.databinding.LayoutTrafficBinding
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.traffic.TrafficColor
import com.yandex.mapkit.traffic.TrafficLevel
import com.yandex.mapkit.traffic.TrafficListener

class TrafficHelper(private val mapView: MapView, private val trafficBinding: LayoutTrafficBinding) {

    private var trafficLevel: TrafficLevel? = null

    private enum class TrafficFreshness {
        Loading, OK, Expired
    }

    private var trafficFreshness: TrafficFreshness? = null

    private var traffic = MapKitFactory.getInstance().createTrafficLayer(mapView.mapWindow)

    private val trafficListener = object: TrafficListener {
        override fun onTrafficChanged(tl: TrafficLevel?) {
            trafficLevel = tl
            trafficFreshness = TrafficFreshness.OK
            updateLevel()
        }

        override fun onTrafficLoading() {
            trafficLevel = null
            trafficFreshness = TrafficFreshness.Loading
            updateLevel()
        }

        override fun onTrafficExpired() {
            trafficLevel = null
            trafficFreshness = TrafficFreshness.Expired
            updateLevel()
        }
    }

    init {
        traffic.isTrafficVisible = true
        traffic.addTrafficListener(trafficListener)
        updateLevel()

        trafficBinding.trafficLight.setOnClickListener {
            traffic.isTrafficVisible = !traffic.isTrafficVisible
            updateLevel()
        }
    }

    private fun updateLevel() {
        val iconId: Int
        var level: String? = ""
        if (!traffic.isTrafficVisible) {
            iconId = R.drawable.icon_traffic_light_dark
        } else if (trafficFreshness == TrafficFreshness.Loading) {
            iconId = R.drawable.icon_traffic_light_violet
        } else if (trafficFreshness == TrafficFreshness.Expired) {
            iconId = R.drawable.icon_traffic_light_blue
        } else if (trafficLevel == null) {  // state is fresh but region has no data
            iconId = R.drawable.icon_traffic_light_grey
        } else {
            iconId = when (trafficLevel?.color) {
                TrafficColor.RED -> R.drawable.icon_traffic_light_red
                TrafficColor.GREEN -> R.drawable.icon_traffic_light_green
                TrafficColor.YELLOW -> R.drawable.icon_traffic_light_yellow
                else -> R.drawable.icon_traffic_light_grey
            }
            level = trafficLevel?.level.toString()
        }
        trafficBinding.trafficLight.setImageBitmap(BitmapFactory.decodeResource(mapView.resources, iconId))
        trafficBinding.trafficLightText.text = level
    }
}