package com.example.yandexmaps.ui.helpers

import android.widget.Toast
import com.example.yandexmaps.R
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.Error
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import java.util.*

class DirectionHelper(private val mapView: MapView) {

    private val drivingRouter = DirectionsFactory.getInstance().createDrivingRouter()
    private lateinit var drivingSession: DrivingSession

    private val routeCollection = mapView.map.mapObjects.addCollection()


    private val drivingRouteListener = object: DrivingSession.DrivingRouteListener {
        override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
            for (route in routes) {
                routeCollection.addPolyline(route.geometry)
            }
        }

        override fun onDrivingRoutesError(error: Error) {
            val errorMessage = mapView.context.getString(
                when(error) {
                    is RemoteError -> R.string.remote_error_message
                    is NetworkError -> R.string.network_error_message
                    else -> R.string.unknown_error_message
                }
            )

            Toast.makeText(mapView.context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }


    fun submitRequest(start: Point, end: Point) {
        val drivingOptions = DrivingOptions()
        val vehicleOptions = VehicleOptions()
        val requestPoints = ArrayList<RequestPoint>()
        requestPoints.add(
            RequestPoint(
                start,
                RequestPointType.WAYPOINT,
                null
            )
        )
        requestPoints.add(
            RequestPoint(
                end,
                RequestPointType.WAYPOINT,
                null
            )
        )
        drivingSession =
            drivingRouter.requestRoutes(requestPoints, drivingOptions, vehicleOptions, drivingRouteListener)
    }
}