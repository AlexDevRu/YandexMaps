package com.example.yandexmaps.ui.helpers

import android.graphics.Color
import android.util.Log
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.geometry.SubpolylineHelper
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.transport.masstransit.Route
import com.yandex.mapkit.transport.masstransit.SectionMetadata.SectionData
import com.yandex.mapkit.transport.masstransit.Transport
import java.util.*

class DirectionHelper(mapView: MapView) {

    companion object {
        private const val TAG = "DirectionHelper"
    }

    private val drivingRouteCollection = mapView.map.mapObjects.addCollection()
    private val massTransitRouteCollection = mapView.map.mapObjects.addCollection()

    fun updateVisibility(isVisible: Boolean) {
        drivingRouteCollection.isVisible = isVisible
        massTransitRouteCollection.isVisible = isVisible
    }

    fun clearRoutes() {
        drivingRouteCollection.clear()
        massTransitRouteCollection.clear()
    }

    fun drawDrivingRoutes(route: DrivingRoute) {
        clearRoutes()
        drivingRouteCollection.addPolyline(route.geometry)
    }

    fun drawMassTransitRoutes(route: Route) {
        clearRoutes()

        for (section in route.sections) {
            drawSection(
                section.metadata.data,
                SubpolylineHelper.subpolyline(
                    route.geometry, section.geometry
                )
            )
        }
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