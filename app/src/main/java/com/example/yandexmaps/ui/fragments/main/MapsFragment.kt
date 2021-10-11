package com.example.yandexmaps.ui.fragments.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.example.yandexmaps.R
import com.example.yandexmaps.args.toArg
import com.example.yandexmaps.databinding.FragmentMapsBinding
import com.example.yandexmaps.ui.fragments.adapters.DrivingAdapter
import com.example.yandexmaps.ui.fragments.adapters.MassTransitAdapter
import com.example.yandexmaps.ui.fragments.base.BaseFragment
import com.example.yandexmaps.ui.helpers.DirectionHelper
import com.example.yandexmaps.ui.helpers.PanoramaHelper
import com.example.yandexmaps.ui.helpers.TrafficHelper
import com.example.yandexmaps.ui.helpers.UserLocationHelper
import com.example.yandexmaps.utils.Utils
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.driving.*
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.BoundingBoxHelper
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.location.FilteringMode
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.logo.Alignment
import com.yandex.mapkit.logo.HorizontalAlignment
import com.yandex.mapkit.logo.VerticalAlignment
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.places.panorama.PanoramaService
import com.yandex.mapkit.search.*
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class MapsFragment: BaseFragment<FragmentMapsBinding>(FragmentMapsBinding::inflate) {

    companion object {
        private const val TAG = "MapsFragment"
    }

    private val viewModel by sharedViewModel<MapsVM>()

    private val cameraListener = CameraListener { _, cameraPosition, _, _ ->
        userLocationHelper.resetAnchor()
        viewModel.cameraPosition = cameraPosition
    }

    private val inputListener = object: InputListener {
        override fun onMapTap(p0: Map, point: Point) {

            Log.e(TAG, "onMapTap ${viewModel.markerMode.value} ${viewModel.directionAction.value}")

            if(viewModel.markerMode.value == MARKER_MODE.PLACE) {
                binding.mapview.map.deselectGeoObject()
                viewModel.selectedGeoObject.value = null
            } else {
                setDirectionMarkerByPoint(point)
            }
        }

        override fun onMapLongTap(p0: Map, p1: Point) {}
    }

    private fun setDirectionMarkerByPoint(point: Point) {
        if(viewModel.directionMarkerType.value == DIRECTION_MARKER_TYPE.ORIGIN) {
            viewModel.directionShouldBeReload = true
            viewModel.origin.value = point
        } else if(viewModel.directionMarkerType.value == DIRECTION_MARKER_TYPE.DESTINATION) {
            viewModel.directionShouldBeReload = true
            viewModel.destination.value = point
        }
    }

    private val geoObjectTapListener = GeoObjectTapListener {

        val selectionMetadata = it.geoObject
            .metadataContainer
            .getItem(GeoObjectSelectionMetadata::class.java)

        val point = it.geoObject.geometry.firstOrNull()?.point

        viewModel.selectedGeoObject.value = it.geoObject

        if(viewModel.markerMode.value == MARKER_MODE.PLACE) {
            Log.w(TAG, "selected point=${point?.latitude},${point?.longitude}; markerMode=${viewModel.markerMode.value}")
            if (selectionMetadata != null) {
                binding.mapview.map.selectGeoObject(selectionMetadata.id, selectionMetadata.layerId)
            }
        } else if(point != null) {
            setDirectionMarkerByPoint(point)
        }

        selectionMetadata != null
    }

    private val locationPermissionResult = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            Log.d(TAG, "onRequestPermissionsResult: permission granted")
            initViews()
        } else {
            Log.e(TAG, "onRequestPermissionsResult: permission failed")
        }
    }

    private lateinit var searchCollection: MapObjectCollection
    private lateinit var originCollection: MapObjectCollection
    private lateinit var destinationCollection: MapObjectCollection

    private lateinit var directionHelper: DirectionHelper
    private lateinit var trafficHelper: TrafficHelper
    private val panoramaHelper = PanoramaHelper()
    private lateinit var userLocationHelper: UserLocationHelper


    private val searchManager = SearchFactory.getInstance().createSearchManager(
        SearchManagerType.COMBINED)

    private lateinit var searchSession: Session

    private val locationManager = MapKitFactory.getInstance().createLocationManager()

    private lateinit var drivingAdapter: DrivingAdapter
    private lateinit var massTransitAdapter: MassTransitAdapter

    private val locationListener = object: LocationListener {
        override fun onLocationUpdated(point: Location) {
            Log.w(TAG, "user location ${point.position.latitude}, ${point.position.longitude}")
            viewModel.userLocation = point.position
            viewModel.applyDirectionAction(DIRECTION_ACTION.BIND_MY_LOCATION, DIRECTION_MARKER_TYPE.ORIGIN)
        }

        override fun onLocationStatusUpdated(p0: LocationStatus) {
            Log.w(TAG, "user location status ${p0}")
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchCollection = binding.mapview.map.mapObjects.addCollection()
        originCollection = binding.mapview.map.mapObjects.addCollection()
        destinationCollection = binding.mapview.map.mapObjects.addCollection()

        directionHelper = DirectionHelper(binding.mapview)
        trafficHelper = TrafficHelper(binding.mapview, binding.trafficView)
        userLocationHelper = UserLocationHelper(binding.mapview) {
            if (viewModel.userAdded) return@UserLocationHelper false
            viewModel.userAdded = true
            return@UserLocationHelper true
        }

        drivingAdapter = DrivingAdapter {

        }

        massTransitAdapter = MassTransitAdapter {

        }

        requestLocationPermission()

        if(viewModel.cameraPosition != null) {
            binding.mapview.map.move(viewModel.cameraPosition!!)
        }
    }


    private fun initViews() {
        initDirectionTypes()

        binding.mapview.map.logo.setAlignment(Alignment(HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM))

        binding.searchButton.setOnClickListener {
            val action = MapsFragmentDirections.actionMapsFragmentToSearchFragment()
            findNavController().navigate(action)
        }

        binding.clearSearchButton.setOnClickListener {
            viewModel.searchResponse.value = null
        }

        binding.mapview.map.addTapListener(geoObjectTapListener)

        binding.mapview.map.addInputListener(inputListener)

        binding.mapview.map.addCameraListener(cameraListener)

        binding.myLocationButton.setOnClickListener {

            if(userLocationHelper.userLocation == null) return@setOnClickListener

            binding.mapview.map.move(
                userLocationHelper.userLocation!!,
                Animation(Animation.Type.SMOOTH, 1f),
                null
            )
        }

        when(viewModel.markerMode.value) {
            MARKER_MODE.PLACE -> {
                binding.root.jumpToState(R.id.hiddenDirectionLayout)
            }
            MARKER_MODE.DIRECTION -> {
                binding.root.jumpToState(R.id.openedDirectionLayout)
            }
        }

        binding.directionsButton.setOnClickListener {
            binding.root.setTransition(R.id.openDirectionLayoutTransition)
            when(viewModel.markerMode.value) {
                MARKER_MODE.PLACE -> {
                    viewModel.markerMode.value = MARKER_MODE.DIRECTION
                    binding.root.transitionToEnd()
                }
                MARKER_MODE.DIRECTION -> {
                    viewModel.markerMode.value = MARKER_MODE.PLACE
                    binding.root.transitionToStart()
                }
            }
        }

        binding.panoramaButton.setOnClickListener {
            val point = viewModel.selectedGeoObject.value?.geometry?.getOrNull(0)?.point

            if(point != null) {
                val action = MapsFragmentDirections.actionMapsFragmentToPanoramaFragment(point.toArg())
                findNavController().navigate(action)
            }
        }

        binding.directionLayout.origin.setOnClickListener {
            Log.e(TAG, "current destination ${findNavController().currentDestination}")
            viewModel.directionMarkerType.value = DIRECTION_MARKER_TYPE.ORIGIN
            openSelectDialog()
        }

        binding.directionLayout.destination.setOnClickListener {
            //if(counter++ % 2 == 1) return@setOnClickListener
            Log.e(TAG, "current destination ${findNavController().currentDestination}")
            viewModel.directionMarkerType.value = DIRECTION_MARKER_TYPE.DESTINATION
            openSelectDialog()
        }

        binding.root.setTransitionListener(object: MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {}

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {}

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if(currentId == R.id.hiddenDirectionLayout) {
                    binding.directionsButton.setImageResource(R.drawable.ic_baseline_directions_24)
                } else {
                    binding.directionsButton.setImageResource(R.drawable.ic_baseline_arrow_back_24)
                }
                if(currentId == R.id.openedDirectionLayout && viewModel.directionBuilded) {
                    binding.root.setTransition(R.id.expandDirectionLayoutTransition)
                }
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {}
        })

        if(viewModel.selectedGeoObject.value != null) {
            val selectionMetadata = viewModel.selectedGeoObject.value!!
                .metadataContainer
                .getItem(GeoObjectSelectionMetadata::class.java)

            binding.mapview.map.selectGeoObject(selectionMetadata.id, selectionMetadata.layerId)
        }

        observe()
    }

    private fun initDirectionTypes() {
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.direction_types,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.directionLayout.directionTypeSpinner.adapter = adapter

        binding.directionLayout.directionTypeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                viewModel.directionType.value = when(p2) {
                    1 -> DIRECTION_TYPE.MASS_TRANSIT
                    else -> DIRECTION_TYPE.DRIVING
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                viewModel.directionType.value = DIRECTION_TYPE.DRIVING
            }

        }
    }

    private fun openSelectDialog() {
        val action = MapsFragmentDirections.actionMapsFragmentToSelectDirectionInputDialog()
        findNavController().navigate(action)
    }

    private val panoramaListener = object: PanoramaService.SearchListener {
        override fun onPanoramaSearchResult(p0: String) {
            Log.e(TAG, "panorama find")
            binding.panoramaButton.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "panorama find", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "panorama button visible ${binding.panoramaButton.visibility == View.VISIBLE}")
        }

        override fun onPanoramaSearchError(error: Error) {
            Log.e(TAG, "panorama not find")
            Toast.makeText(requireContext(), "panorama NOT find", Toast.LENGTH_SHORT).show()
            binding.panoramaButton.visibility = View.GONE
        }
    }

    private fun observe() {
        viewModel.searchResponse.observe(viewLifecycleOwner) searchObserver@ {
            searchCollection.clear()

            if(it == null) {
                binding.searchInfoContainer.visibility = View.GONE
                return@searchObserver
            }

            if(viewModel.markerMode.value == MARKER_MODE.PLACE) {
                binding.searchInfoContainer.visibility = View.VISIBLE
            }

            val response = it.response

            binding.searchQuery.text = response.metadata.requestText

            var i = 0

            for (searchResult in response.collection.children) {
                val resultLocation = searchResult.obj?.geometry?.get(0)?.point
                if (resultLocation != null) {
                    if(i++ >= it.showResults) break
                    if(viewModel.markerMode.value == MARKER_MODE.PLACE) {
                        searchCollection.addPlacemark(
                            resultLocation,
                            ImageProvider.fromResource(requireContext(), R.drawable.search_result)
                        )
                    } else {
                        when (viewModel.directionMarkerType.value) {
                            DIRECTION_MARKER_TYPE.ORIGIN -> {
                                viewModel.applyDirectionAction(DIRECTION_ACTION.CHOOSE_ON_MAP, DIRECTION_MARKER_TYPE.ORIGIN)
                                viewModel.origin.value = resultLocation
                            }
                            DIRECTION_MARKER_TYPE.DESTINATION -> {
                                viewModel.applyDirectionAction(DIRECTION_ACTION.CHOOSE_ON_MAP, DIRECTION_MARKER_TYPE.DESTINATION)
                                viewModel.destination.value = resultLocation
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "response observer")

            response.collection.children.firstOrNull()?.let {
                val point = it.obj?.geometry?.get(0)?.point
                if(point != null)
                    binding.mapview.mapWindow.map.move(CameraPosition(point, 11f, 0f, 0f))
            }
        }

        viewModel.selectedGeoObject.observe(viewLifecycleOwner) {
            if(it != null) {
                val point = it.geometry.getOrNull(0)?.point
                if(viewModel.markerMode.value == MARKER_MODE.PLACE) {
                    if(point != null) {
                        panoramaHelper.findNearest(point, panoramaListener)

                        //val metadata = it.metadataContainer.getItem(BusinessObjectMetadata::class.java)

                        searchByPoint(it.geometry.first().point!!, {
                            Log.d(TAG, "collection ${it.collection.children.first().obj}")
                            val obj = it.collection.children.first().obj
                            val metadata = obj?.metadataContainer?.getItem(BusinessObjectMetadata::class.java)
                            Log.d(TAG, "selected address ${metadata?.address?.formattedAddress}")
                            Log.d(TAG, "selected working hours ${metadata?.workingHours?.availabilities?.firstOrNull()?.days} ${metadata?.workingHours?.availabilities?.firstOrNull()?.timeRanges?.firstOrNull()?.from}")
                        }, {
                            Log.d(TAG, "selected error")
                        })
                    }
                    else {
                        Log.e(TAG, "point == null")
                        binding.panoramaButton.visibility = View.GONE
                    }
                } else {
                    Log.e(TAG, "direction ${point}")
                    if(point != null)
                        viewModel.destination.value = point
                }

            } else {
                Log.e(TAG, "point null")
                binding.panoramaButton.visibility = View.GONE
            }
        }

        viewModel.origin.observe(viewLifecycleOwner) {
            originCollection.clear()
            if(it != null) {
                Log.w(TAG, "origin set $it")
                originCollection.addPlacemark(it, ImageProvider.fromResource(requireContext(), R.drawable.origin))
                searchByPoint(it, { response ->
                    viewModel.originAddress.value = response.collection.children.firstOrNull()?.obj?.name
                }, {
                    viewModel.originAddress.value = null
                })
                buildDirection()
            } else {
                viewModel.originAddress.value = resources.getString(R.string.choose_origin)
            }
        }

        viewModel.destination.observe(viewLifecycleOwner) {
            destinationCollection.clear()
            if(it != null) {
                Log.w(TAG, "destination set $it")
                destinationCollection.addPlacemark(it, ImageProvider.fromResource(requireContext(), R.drawable.destination))
                searchByPoint(it, { response ->
                    viewModel.destinationAddress.value = response.collection.children.firstOrNull()?.obj?.name
                }, {
                    viewModel.destinationAddress.value = null
                })
                buildDirection()
            } else {
                viewModel.destinationAddress.value = resources.getString(R.string.choose_destination)
            }
        }

        viewModel.markerMode.observe(viewLifecycleOwner) {
            Log.d(TAG, "marker mode observer ${it}")
            searchCollection.isVisible = it == MARKER_MODE.PLACE
            originCollection.isVisible = it != MARKER_MODE.PLACE
            destinationCollection.isVisible = it != MARKER_MODE.PLACE
            directionHelper.updateVisibility(it != MARKER_MODE.PLACE)

            binding.searchInfoContainer.visibility =
                if(viewModel.searchResponse.value != null && it == MARKER_MODE.PLACE) View.VISIBLE else View.GONE
        }

        viewModel.originAddress.observe(viewLifecycleOwner) {
            Log.d(TAG, "originAddress")
            binding.directionLayout.origin.text = it
        }

        viewModel.destinationAddress.observe(viewLifecycleOwner) {
            Log.d(TAG, "destinationAddress")
            binding.directionLayout.destination.text = it
        }

        viewModel.directionType.observe(viewLifecycleOwner) {
            if(viewModel.markerMode.value == MARKER_MODE.DIRECTION) {
                viewModel.directionShouldBeReload = true
                buildDirection()
            }
        }

        viewModel.drivingRoutes.observe(viewLifecycleOwner) drivingRoutes@ { routes ->
            if(routes == null) return@drivingRoutes

            if(!checkDirectionFound(routes)) return@drivingRoutes

            if(viewModel.markerMode.value == MARKER_MODE.DIRECTION) {
                val boundingBox = BoundingBoxHelper.getBounds(routes.first().geometry)
                boundCameraToDirection(boundingBox)
            }

            var distance = 0.0
            var time = 0.0

            val sections = mutableListOf<DrivingSectionMetadata>()

            routes.first().sections.forEach { route ->
                distance += route.metadata.weight.distance.value
                time += route.metadata.weight.time.value
                sections.add(route.metadata)
            }

            showTotalDirectionData(distance, time)

            binding.directionLayout.routesList.adapter = drivingAdapter

            drivingAdapter.submitList(sections)
        }

        viewModel.massTransitRoutes.observe(viewLifecycleOwner) massTransitObserver@ { routes ->
            if(routes == null) return@massTransitObserver

            if(!checkDirectionFound(routes)) return@massTransitObserver

            if(viewModel.markerMode.value == MARKER_MODE.DIRECTION) {
                val boundingBox = BoundingBoxHelper.getBounds(routes.first().geometry)
                boundCameraToDirection(boundingBox)
            }

            var distance = 0.0
            var time = 0.0

            routes.first().sections.forEach { section ->
                distance += section.metadata.weight.walkingDistance.value
                time += section.metadata.weight.time.value
            }

            showTotalDirectionData(distance, time)

            binding.directionLayout.routesList.adapter = massTransitAdapter

            massTransitAdapter.submitList(routes.first().sections.filter {
                !(it.metadata.weight.walkingDistance.value.toInt() == 0 && it.metadata.data.transports == null)
            })
        }
    }

    private fun searchByPoint(point: Point, onResponse: (Response) -> Unit, onFailure: (Error) -> Unit) {
        searchPointListener = MySearchPointListener(onResponse, onFailure)
        searchSession = searchManager.submit(
            point,
            binding.mapview.map.cameraPosition.zoom.toInt(),
            SearchOptions().setSearchTypes(SearchType.GEO.value),
            searchPointListener
        )
    }

    inner class MySearchPointListener(private val onResponse: (Response) -> Unit, private val onFailure: (Error) -> Unit): Session.SearchListener {
        override fun onSearchResponse(response: Response) {
            onResponse(response)
        }

        override fun onSearchError(error: Error) {
            showSnackBar(error.toString())
            onFailure(error)
        }
    }

    private lateinit var searchPointListener: MySearchPointListener

    private fun buildDirection() {
        val originPoint = viewModel.origin.value
        val destinationPoint = viewModel.destination.value

        if(originPoint != null && destinationPoint != null && viewModel.directionShouldBeReload) {
            viewModel.directionShouldBeReload = false

            binding.directionLayout.progressBar.visibility = View.VISIBLE

            when(viewModel.directionType.value) {
                DIRECTION_TYPE.DRIVING -> {
                    directionHelper.submitDrivingRequest(originPoint, destinationPoint, {
                        binding.directionLayout.progressBar.visibility = View.GONE
                        viewModel.directionBuilded = true
                        binding.root.setTransition(R.id.expandDirectionLayoutTransition)

                        viewModel.massTransitRoutes.value = null
                        viewModel.drivingRoutes.value = it
                    }) {
                        binding.directionLayout.progressBar.visibility = View.GONE
                        viewModel.directionBuilded = false
                    }
                }
                DIRECTION_TYPE.MASS_TRANSIT -> {
                    directionHelper.submitMassTransitRequest(originPoint, destinationPoint, {
                        binding.directionLayout.progressBar.visibility = View.GONE
                        viewModel.directionBuilded = true
                        binding.root.setTransition(R.id.expandDirectionLayoutTransition)

                        viewModel.drivingRoutes.value = null
                        viewModel.massTransitRoutes.value = it
                    }) {
                        binding.directionLayout.progressBar.visibility = View.GONE
                        viewModel.directionBuilded = false
                    }
                }
            }
        }
    }

    private fun <T> checkDirectionFound(routes: List<T>): Boolean {
        updateDirectionErrorVisibility(routes.isEmpty())
        return routes.isNotEmpty()
    }

    private fun updateDirectionErrorVisibility(visible: Boolean = false) {
        val visibilityError = if(visible) View.VISIBLE else View.GONE
        val visibility = if(visible) View.GONE else View.VISIBLE

        binding.directionLayout.distance.visibility = visibility
        binding.directionLayout.duration.visibility = visibility
        binding.directionLayout.routesList.visibility = visibility
        binding.directionLayout.directionNotFound.visibility = visibilityError
    }

    private fun boundCameraToDirection(boundingBox: BoundingBox) {
        var cameraPosition = binding.mapview.map.cameraPosition(boundingBox)
        cameraPosition = CameraPosition(cameraPosition.target, cameraPosition.zoom - 0.8f, cameraPosition.azimuth, cameraPosition.tilt)
        binding.mapview.map.move(cameraPosition, Animation(Animation.Type.SMOOTH, 1f), null)
    }

    private fun showTotalDirectionData(distance: Double, time: Double) {
        val totalDistance = Utils.formatDistance(requireContext(), distance)
        val totalDuration = Utils.formatDuration(requireContext(), time)

        binding.directionLayout.distance.text = resources.getString(R.string.total_distance, totalDistance)
        binding.directionLayout.duration.text = resources.getString(R.string.total_duration, totalDuration)
    }


    private fun coarseAndFineLocationPermissionsIsGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions")
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if(coarseAndFineLocationPermissionsIsGranted()) {
            locationManager.subscribeForLocationUpdates(0.0, 3000, 2.0, true, FilteringMode.OFF, viewModel.locationUpdateListener)
            locationManager.requestSingleUpdate(locationListener)
            initViews()
        } else {
            locationPermissionResult.launch(permissions)
        }
    }


    override fun onStop() {
        binding.mapview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapview.onStart()
    }
}