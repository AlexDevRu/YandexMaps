package com.example.yandexmaps.ui.helpers

import android.graphics.Color
import android.util.Log
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
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.geometry.SubpolylineHelper
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.*
import com.yandex.mapkit.transport.masstransit.SectionMetadata.SectionData
import com.yandex.runtime.Error
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import java.util.*

class DirectionHelper(private val mapView: MapView) {

    companion object {
        private const val TAG = "DirectionHelper"
    }

    private val drivingRouter = DirectionsFactory.getInstance().createDrivingRouter()
    private lateinit var drivingSession: DrivingSession

    private var onDrivingRoutesCallback: (List<DrivingRoute>) -> Unit = {}
    private var onDrivingErrorCallback: (Error) -> Unit = {}

    private var onMassRoutesCallback: (List<Route>) -> Unit = {}
    private var onMassErrorCallback: (Error) -> Unit = {}

    private val drivingRouteCollection = mapView.map.mapObjects.addCollection()
    private val massTransitRouteCollection = mapView.map.mapObjects.addCollection()

    private val mtRouter = TransportFactory.getInstance().createMasstransitRouter()

    fun updateVisibility(isVisible: Boolean) {
        drivingRouteCollection.isVisible = isVisible
        massTransitRouteCollection.isVisible = isVisible
    }

    /*fun deleteDrivingRoutes() {
        drivingRouteCollection.clear()
    }

    fun deleteMassTransitRoutes() {
        massTransitRouteCollection.clear()
    }*/


    private val drivingRouteListener = object: DrivingSession.DrivingRouteListener {
        override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
            drivingRouteCollection.clear()
            massTransitRouteCollection.clear()

            for (route in routes) {
                drivingRouteCollection.addPolyline(route.geometry)
            }
            onDrivingRoutesCallback(routes)
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

            onDrivingErrorCallback(error)
        }
    }

    private val mtRouteListener = object: Session.RouteListener {
        override fun onMasstransitRoutes(routes: MutableList<Route>) {
            // In this example we consider first alternative only
            massTransitRouteCollection.clear()
            drivingRouteCollection.clear()

            if (routes.size > 0) {
                for (section in routes[0].sections) {
                    if (section.stops.size > 0) Log.e("asd", "stop" + section.stops[0].stop.name)
                    drawSection(
                        section.metadata.data,
                        SubpolylineHelper.subpolyline(
                            routes[0].geometry, section.geometry
                        )
                    )
                }
            }

            onMassRoutesCallback(routes)
        }

        override fun onMasstransitRoutesError(error: Error) {
            val errorMessage = mapView.context.getString(
                when(error) {
                    is RemoteError -> R.string.remote_error_message
                    is NetworkError -> R.string.network_error_message
                    else -> R.string.unknown_error_message
                }
            )

            Toast.makeText(mapView.context, errorMessage, Toast.LENGTH_SHORT).show()

            onMassErrorCallback(error)
        }

    }


    fun submitDrivingRequest(
        start: Point, end: Point,
        onDrivingRoutesCallback: (List<DrivingRoute>) -> Unit,
        onErrorCallback: (Error) -> Unit,
    ) {
        this.onDrivingRoutesCallback = onDrivingRoutesCallback
        this.onDrivingErrorCallback = onErrorCallback

        val drivingOptions = DrivingOptions().setRoutesCount(1)
        val vehicleOptions = VehicleOptions()
        val requestPoints = ArrayList<RequestPoint>(2)
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


    fun submitMassTransitRequest(
        start: Point, end: Point,
        onMassRoutesCallback: (List<Route>) -> Unit,
        onMassErrorCallback: (Error) -> Unit,
    ) {
        this.onMassRoutesCallback = onMassRoutesCallback
        this.onMassErrorCallback = onMassErrorCallback

        val options = MasstransitOptions(
            ArrayList(),
            ArrayList(),
            TimeOptions()
        )
        val points = ArrayList<RequestPoint>(2)
        points.add(RequestPoint(start, RequestPointType.WAYPOINT, null))
        points.add(RequestPoint(end, RequestPointType.WAYPOINT, null))
        mtRouter.requestRoutes(points, options, mtRouteListener)
    }

    private fun drawSection(
        data: SectionData,
        geometry: Polyline
    ) {
        // Draw a section polyline on a map
        // Set its color depending on the information which the section contains
        val polylineMapObject = massTransitRouteCollection.addPolyline(geometry)
        // Masstransit route section defines exactly one on the following
        // 1. Wait until public transport unit arrives
        // 2. Walk
        // 3. Transfer to a nearby stop (typically transfer to a connected
        //    underground station)
        // 4. Ride on a public transport
        // Check the corresponding object for null to get to know which
        // kind of section it is
        if (data.transports != null) {
            // A ride on a public transport section contains information about
            // all known public transport lines which can be used to travel from
            // the start of the section to the end of the section without transfers
            // along a similar geometry
            for (transport in data.transports!!) {
                // Some public transport lines may have a color associated with them
                // Typically this is the case of underground lines
                if (transport.line.style != null) {
                    polylineMapObject.strokeColor = transport.line.style!!.color!! or -0x1000000
                    return
                }
                Log.w(TAG, "transport types ${transport.line.vehicleTypes}")
            }
            Log.w(TAG, "------------------------------")
            // Let us draw bus lines in green and tramway lines in red
            // Draw any other public transport lines in blue
            val knownVehicleTypes = HashSet<String>()
            knownVehicleTypes.add("bus")
            knownVehicleTypes.add("tramway")
            knownVehicleTypes.add("trolleybus")
            knownVehicleTypes.add("minibus")
            knownVehicleTypes.add("railway")
            for (transport in data.transports!!) {
                when (getVehicleType(transport, knownVehicleTypes)) {
                    "bus" -> {
                        polylineMapObject.strokeColor = Color.CYAN
                        return
                    }
                    "tramway" -> {
                        polylineMapObject.strokeColor = Color.RED
                        return
                    }
                    "trolleybus" -> {
                        polylineMapObject.strokeColor = Color.BLUE
                        return
                    }
                    "minibus" -> {
                        polylineMapObject.strokeColor = Color.MAGENTA
                        return
                    }
                    "railway" -> {
                        polylineMapObject.strokeColor = Color.GREEN
                        return
                    }
                }
            }
            polylineMapObject.strokeColor = Color.YELLOW
        } else {
            // This is not a public transport ride section
            // In this example let us draw it in black
            polylineMapObject.strokeColor = Color.BLACK
        }
    }

    private fun getVehicleType(transport: Transport, knownVehicleTypes: HashSet<String>): String? {
        for (type in transport.line.vehicleTypes) {
            if (knownVehicleTypes.contains(type)) {
                return type
            }
        }
        return null
    }
}