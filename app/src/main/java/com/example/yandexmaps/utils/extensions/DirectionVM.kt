package com.example.yandexmaps.utils.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.yandexmaps.ui.fragments.main.Result
import com.example.yandexmaps.utils.Utils
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.MasstransitOptions
import com.yandex.mapkit.transport.masstransit.Route
import com.yandex.mapkit.transport.masstransit.Session
import com.yandex.mapkit.transport.masstransit.TimeOptions
import com.yandex.runtime.Error
import java.util.*

class DirectionVM {

    private val _drivingRoutes = MutableLiveData<Result<DirectionModel<DrivingRoute>>>()
    val drivingRoutes: LiveData<Result<DirectionModel<DrivingRoute>>> = _drivingRoutes

    private val _massTransitRoutes = MutableLiveData<Result<DirectionModel<Route>>>()
    val massTransitRoutes: LiveData<Result<DirectionModel<Route>>> = _massTransitRoutes

    private val drivingRouter = DirectionsFactory.getInstance().createDrivingRouter()
    private lateinit var drivingSession: DrivingSession


    private val mtRouter = TransportFactory.getInstance().createMasstransitRouter()

    fun submitDrivingRequest(start: Point, end: Point) {
        _drivingRoutes.value = Result.Loading()

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

    private val drivingRouteListener = object: DrivingSession.DrivingRouteListener {
        override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
            if(routes.isEmpty()) {
                _drivingRoutes.value = Result.Failure(DirectionNotFound())
            } else {
                val firstRoute = routes.first()

                var distance = 0.0
                var duration = 0.0

                routes.first().sections.forEach { route ->
                    distance += route.metadata.weight.distance.value
                    duration += route.metadata.weight.time.value
                }

                val model = DirectionModel(distance = distance, duration = duration, firstRoute)
                _drivingRoutes.value = Result.Success(model)
            }
        }

        override fun onDrivingRoutesError(error: Error) {
            val errorMessage = Utils.getErrorMessage(error)
            _drivingRoutes.value = Result.Failure(Exception(errorMessage.toString()))
        }
    }

    private val mtRouteListener = object: Session.RouteListener {
        override fun onMasstransitRoutes(routes: MutableList<Route>) {
            if(routes.isEmpty()) {
                _drivingRoutes.value = Result.Failure(DirectionNotFound())
            } else {
                val firstRoute = routes.first()

                var distance = 0.0
                var duration = 0.0

                routes.first().sections.forEach { route ->
                    distance += route.metadata.weight.walkingDistance.value
                    duration += route.metadata.weight.time.value
                }

                val model = DirectionModel(distance = distance, duration = duration, firstRoute)
                _massTransitRoutes.value = Result.Success(model)
            }
        }

        override fun onMasstransitRoutesError(error: Error) {
            val errorMessage = Utils.getErrorMessage(error)
            _massTransitRoutes.value = Result.Failure(Exception(errorMessage.toString()))
        }
    }

    fun submitMassTransitRequest(
        start: Point, end: Point
    ) {
        _massTransitRoutes.value = Result.Loading()

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
}

class DirectionNotFound: Exception() {

}

data class DirectionModel<T> (
    val distance: Double,
    val duration: Double,
    val route: T
)