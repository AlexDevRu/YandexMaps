package com.example.yandexmaps.ui.fragments.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.yandexmaps.R
import com.example.yandexmaps.args.toArg
import com.example.yandexmaps.databinding.FragmentMapsBinding
import com.example.yandexmaps.ui.fragments.adapters.DrivingAdapter
import com.example.yandexmaps.ui.fragments.adapters.MassTransitAdapter
import com.example.yandexmaps.ui.fragments.adapters.viewpager.PlaceTabsAdapter
import com.example.yandexmaps.ui.fragments.base.BaseFragment
import com.example.yandexmaps.ui.fragments.search.SearchVM
import com.example.yandexmaps.ui.helpers.DirectionHelper
import com.example.yandexmaps.ui.helpers.TrafficHelper
import com.example.yandexmaps.ui.helpers.UserLocationHelper
import com.example.yandexmaps.ui.models.SearchResponseModel
import com.example.yandexmaps.utils.Utils
import com.example.yandexmaps.utils.extensions.DirectionNotFound
import com.example.yandexmaps.utils.extensions.moveCameraToBoundingBox
import com.example.yandexmaps.utils.extensions.visibleRegion
import com.google.android.material.tabs.TabLayout
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.driving.*
import com.yandex.mapkit.geometry.BoundingBoxHelper
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.logo.Alignment
import com.yandex.mapkit.logo.HorizontalAlignment
import com.yandex.mapkit.logo.VerticalAlignment
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.search.*
import com.yandex.runtime.image.ImageProvider
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class MapsFragment: BaseFragment<FragmentMapsBinding>(FragmentMapsBinding::inflate) {

    companion object {
        private const val TAG = "MapsFragment"
    }

    private val viewModel by sharedViewModel<MapsVM>()
    private val searchVM by sharedViewModel<SearchVM>()

    private val cameraListener = CameraListener { _, cameraPosition, _, finished ->
        userLocationHelper.resetAnchor()
        viewModel.updateCameraPosition(cameraPosition)
        viewModel.updateVisibleRegion(binding.mapview.visibleRegion())
        if(finished) {
            val currentSearchResponse = (viewModel.searchResponse.value as? Result.Success<SearchResponseModel?>)?.value
            Log.e(TAG, "currentSearchResponse $currentSearchResponse")
            Log.e(TAG, "currentSearchResponse ${viewModel.searchResponse.value is Result.Success}")
            Log.e(TAG, "currentSearchResponse ${viewModel.searchResponse.value is Result.Loading}")
            Log.e(TAG, "currentSearchResponse ${viewModel.searchResponse.value is Result.Failure}")
            if(currentSearchResponse?.searchLayer == true)
                viewModel.searchByQuery(currentSearchResponse.searchText)
        }
    }

    private val inputListener = object: InputListener {
        override fun onMapTap(p0: Map, point: Point) {
            Log.e(TAG, "onMapTap ${viewModel.markerMode.value} ${viewModel.directionAction.value}")
            viewModel.setMarker(point)
        }

        override fun onMapLongTap(p0: Map, p1: Point) {}
    }

    private val geoObjectTapListener = GeoObjectTapListener {

        val selectionMetadata = it.geoObject
            .metadataContainer
            .getItem(GeoObjectSelectionMetadata::class.java)

        viewModel.setMarker(it.geoObject)

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
    private lateinit var userLocationHelper: UserLocationHelper

    private lateinit var drivingAdapter: DrivingAdapter
    private lateinit var massTransitAdapter: MassTransitAdapter

    private lateinit var routesList: RecyclerView


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchCollection = binding.mapview.map.mapObjects.addCollection()
        originCollection = binding.mapview.map.mapObjects.addCollection()
        destinationCollection = binding.mapview.map.mapObjects.addCollection()

        directionHelper = DirectionHelper(binding.mapview)
        trafficHelper = TrafficHelper(binding.mapview, binding.trafficView)
        userLocationHelper = UserLocationHelper(binding.mapview) {
            val oldValue = viewModel.userLocationVM.userAdded
            viewModel.userLocationVM.userAdded()
            return@UserLocationHelper oldValue
        }

        drivingAdapter = DrivingAdapter {

        }

        massTransitAdapter = MassTransitAdapter {

        }

        routesList = binding.routesList ?: binding.directionLayout.routesList!!

        requestLocationPermission()
    }


    private fun initViews() {
        initDirectionTypes()
        initViewPager()

        binding.mapview.map.logo.setAlignment(Alignment(HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM))

        binding.searchButton.setOnClickListener {
            val action = MapsFragmentDirections.actionMapsFragmentToSearchFragment()
            findNavController().navigate(action)
        }

        binding.clearSearchButton.setOnClickListener {
            viewModel.clearSearch()
        }

        binding.mapview.map.addTapListener(geoObjectTapListener)

        binding.mapview.map.addInputListener(inputListener)

        binding.mapview.map.addCameraListener(cameraListener)

        binding.myLocationButton.setOnClickListener {

            if(viewModel.userLocationVM.userLocation.value == null) return@setOnClickListener

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
                binding.root.setTransition(R.id.expandDirectionLayoutTransition)
            }
        }

        binding.directionsButton.setOnClickListener {
            viewModel.toggleMarkerMode()
        }

        binding.panoramaButton.setOnClickListener {
            val result = viewModel.selectedGeoObjectMetadata.value
            if(result is Result.Success) {
                val point = result.value.panoramasMetadata?.panoramas?.firstOrNull()?.point

                if(point != null) {
                    val action = MapsFragmentDirections.actionMapsFragmentToPanoramaFragment(point.toArg())
                    findNavController().navigate(action)
                }
            }
        }

        binding.directionLayout.origin.setOnClickListener {
            Log.e(TAG, "current destination ${findNavController().currentDestination}")
            viewModel.updateDirectionMarkerType(DIRECTION_MARKER_TYPE.ORIGIN)
            openSelectDialog()
        }

        binding.directionLayout.destination.setOnClickListener {
            Log.e(TAG, "current destination ${findNavController().currentDestination}")
            viewModel.updateDirectionMarkerType(DIRECTION_MARKER_TYPE.DESTINATION)
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
                if(currentId == R.id.openedDirectionLayout || currentId == R.id.expandedDirectionLayout) {
                    binding.directionsButton.setImageResource(R.drawable.ic_baseline_arrow_back_24)
                } else {
                    binding.directionsButton.setImageResource(R.drawable.ic_baseline_directions_24)
                }
                if(currentId == R.id.openedDirectionLayout) {
                    binding.root.setTransition(R.id.expandDirectionLayoutTransition)
                }
                if(currentId == R.id.visiblePlaceLayout) {
                    Log.w(TAG, "visible place info")
                    binding.root.setTransition(R.id.expandPlaceLayoutTransition)
                }
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {}
        })

        if(viewModel.cameraPosition.value != null) {
            binding.mapview.map.move(viewModel.cameraPosition.value!!)
        }

        observe()
    }

    private fun initViewPager() {
        binding.placeLayout.viewpager.adapter = PlaceTabsAdapter(childFragmentManager, lifecycle)

        binding.placeLayout.tabs.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if(tab != null) {
                    binding.placeLayout.viewpager.currentItem = tab.position
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        binding.placeLayout.viewpager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.placeLayout.tabs.selectTab(binding.placeLayout.tabs.getTabAt(position))
            }
        })
    }

    private fun initDirectionTypes() {
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.direction_types,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.directionLayout.directionTypeSpinner.adapter = adapter

        val position = when(viewModel.directionType.value) {
            DIRECTION_TYPE.MASS_TRANSIT -> 1
            else -> 0
        }

        binding.directionLayout.directionTypeSpinner.setSelection(position, false)

        binding.directionLayout.directionTypeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Log.w("asd", "spinner item selected ${p2}")
                viewModel.setDirectionType(when(p2) {
                    1 -> DIRECTION_TYPE.MASS_TRANSIT
                    else -> DIRECTION_TYPE.DRIVING
                })
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun openSelectDialog() {
        val action = MapsFragmentDirections.actionMapsFragmentToSelectDirectionInputDialog()
        findNavController().navigate(action)
    }

    private fun observe() {

        viewModel.searchResponse.observe(viewLifecycleOwner) searchObserver@ {

            when(it) {
                is Result.Success -> {
                    searchCollection.clear()

                    if(it.value == null) {
                        binding.searchInfoContainer.visibility = View.GONE
                        return@searchObserver
                    }

                    if(viewModel.markerMode.value == MARKER_MODE.PLACE) {
                        binding.searchInfoContainer.visibility = View.VISIBLE
                    }

                    val response = it.value.response

                    binding.searchQuery.text = it.value.searchText

                    var i = 0

                    for (searchResult in response.collection.children) {
                        val resultLocation = searchResult.obj?.geometry?.get(0)?.point
                        if (resultLocation != null) {
                            if(i++ >= it.value.showResults && !it.value.searchLayer) break
                            if(viewModel.markerMode.value == MARKER_MODE.PLACE) {
                                searchCollection.addPlacemark(
                                    resultLocation,
                                    ImageProvider.fromResource(requireContext(), R.drawable.search_result)
                                )
                            } else {
                                when (viewModel.directionMarkerType.value) {
                                    DIRECTION_MARKER_TYPE.ORIGIN -> {
                                        viewModel.applyDirectionAction(DIRECTION_ACTION.CHOOSE_ON_MAP, DIRECTION_MARKER_TYPE.ORIGIN)
                                        viewModel.setOrigin(resultLocation)
                                    }
                                    DIRECTION_MARKER_TYPE.DESTINATION -> {
                                        viewModel.applyDirectionAction(DIRECTION_ACTION.CHOOSE_ON_MAP, DIRECTION_MARKER_TYPE.DESTINATION)
                                        viewModel.setDestination(resultLocation)
                                    }
                                }
                            }
                        }
                    }

                    Log.d(TAG, "response observer")

                    if(!it.value.searchLayer) {
                        response.collection.children.firstOrNull()?.obj?.boundingBox?.let {
                            Log.e(TAG, "search response bounding box ${it}")
                            var cameraPosition = binding.mapview.map.cameraPosition(it)
                            cameraPosition = CameraPosition(cameraPosition.target, cameraPosition.zoom - 0.7f, cameraPosition.azimuth, cameraPosition.tilt)
                            binding.mapview.mapWindow.map.move(cameraPosition)
                        }
                    }
                }
                is Result.Failure -> {
                    showSnackBar(it.throwable.message!!.toInt())
                }
                is Result.Loading -> {

                }
            }
        }

        viewModel.selectedGeoObject.observe(viewLifecycleOwner) selectedGeoObjectObserver@ {
            if(it != null) {
                val selectionMetadata = it.metadataContainer
                    .getItem(GeoObjectSelectionMetadata::class.java)

                binding.mapview.map.selectGeoObject(selectionMetadata.id, selectionMetadata.layerId)
            } else {
                binding.mapview.map.deselectGeoObject()
            }
        }

        viewModel.selectedGeoObjectMetadata.observe(viewLifecycleOwner) {
            when(it) {
                is Result.Success -> {
                    if(viewModel.markerMode.value == MARKER_MODE.PLACE) {
                        if(binding.root.currentState == R.id.visiblePlaceLayout) {
                            binding.root.setTransition(R.id.expandPlaceLayoutTransition)
                        } else {
                            binding.root.setTransition(R.id.openPlaceLayoutTransition)
                            binding.root.transitionToEnd()
                        }
                    }
                    binding.panoramaButton.visibility = View.VISIBLE
                }
                is Result.Failure -> {
                    binding.panoramaButton.visibility = View.GONE
                    showSnackBar(it.throwable.message!!.toInt())
                }
                is Result.Loading -> {

                }
                else -> {
                    binding.root.setTransition(R.id.openPlaceLayoutTransition)
                    binding.root.jumpToState(R.id.hiddenPlaceLayout)
                }
            }

            binding.placeLayout.progressBar.visibility = if(it is Result.Loading) View.VISIBLE else View.GONE
        }

        viewModel.origin.observe(viewLifecycleOwner) {
            originCollection.clear()
            if(it != null) {
                Log.w(TAG, "origin set $it")
                originCollection.addPlacemark(it, ImageProvider.fromResource(requireContext(), R.drawable.origin))
            } else {
                originCollection.clear()
            }
        }

        viewModel.destination.observe(viewLifecycleOwner) {
            destinationCollection.clear()
            if(it != null) {
                Log.w(TAG, "destination set $it")
                destinationCollection.addPlacemark(it, ImageProvider.fromResource(requireContext(), R.drawable.destination))
            } else {
                destinationCollection.clear()
            }
        }

        viewModel.markerMode.observe(viewLifecycleOwner) {

            when(it) {
                MARKER_MODE.PLACE -> {
                    if(binding.root.currentState == R.id.openedDirectionLayout) {
                        binding.root.setTransition(R.id.openDirectionLayoutTransition)
                        binding.root.transitionToStart()
                    }
                }
                MARKER_MODE.DIRECTION -> {
                    binding.root.setTransition(R.id.openDirectionLayoutTransition)
                    binding.root.transitionToEnd()
                }
            }

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
            when(it) {
               is Result.Success -> {
                   binding.directionLayout.origin.text = it.value ?: getString(R.string.choose_origin)
               }
               is Result.Loading -> {

               }
               is Result.Failure -> {
                   binding.directionLayout.origin.text = getString(R.string.choose_origin)
                   showSnackBar(it.throwable.message.orEmpty())
               }
           }
        }

        viewModel.destinationAddress.observe(viewLifecycleOwner) {
            when(it) {
                is Result.Success -> {
                    Log.d(TAG, "destinationAddress")
                    binding.directionLayout.destination.text = it.value ?: getString(R.string.choose_destination)
                }
                is Result.Loading -> {

                }
                is Result.Failure -> {
                    binding.directionLayout.destination.text = getString(R.string.choose_destination)
                }
            }
        }

        viewModel.directionType.observe(viewLifecycleOwner) {

        }

        viewModel.directionVM.drivingRoutes.observe(viewLifecycleOwner) drivingRoutes@ {
            when(it) {
                is Result.Success -> {
                    directionHelper.drawDrivingRoutes(it.value.route)

                    if(viewModel.markerMode.value == MARKER_MODE.DIRECTION) {
                        val boundingBox = BoundingBoxHelper.getBounds(it.value.route.geometry)
                        binding.mapview.moveCameraToBoundingBox(boundingBox)
                    }

                    showTotalDirectionData(it.value.distance, it.value.duration)

                    val sections = it.value.route.sections.map { it.metadata }

                    routesList.adapter = drivingAdapter

                    drivingAdapter.submitList(sections)
                }
                is Result.Failure -> {
                    directionHelper.clearRoutes()
                }
                is Result.Loading -> {

                }
            }

            updateDirectionErrorVisibility(it is Result.Failure && it.throwable is DirectionNotFound)
        }

        viewModel.directionVM.massTransitRoutes.observe(viewLifecycleOwner) massTransitObserver@ {
            when(it) {
                is Result.Success -> {
                    directionHelper.drawMassTransitRoutes(it.value.route)

                    if(viewModel.markerMode.value == MARKER_MODE.DIRECTION) {
                        val boundingBox = BoundingBoxHelper.getBounds(it.value.route.geometry)
                        binding.mapview.moveCameraToBoundingBox(boundingBox)
                    }

                    showTotalDirectionData(it.value.distance, it.value.duration)

                    val sections = it.value.route.sections.filter {
                        !(it.metadata.weight.walkingDistance.value.toInt() == 0 && it.metadata.data.transports == null)
                    }

                    routesList.adapter = massTransitAdapter

                    massTransitAdapter.submitList(sections)
                }
                is Result.Failure -> {
                    directionHelper.clearRoutes()
                }
                is Result.Loading -> {

                }
            }

            updateDirectionErrorVisibility(it is Result.Failure && it.throwable is DirectionNotFound)
        }
    }

    private fun updateDirectionErrorVisibility(visible: Boolean = false) {
        val visibilityError = if(visible) View.VISIBLE else View.GONE
        val visibility = if(visible) View.GONE else View.VISIBLE

        binding.directionLayout.distance.visibility = visibility
        binding.directionLayout.duration.visibility = visibility
        routesList.visibility = visibility
        binding.directionLayout.directionNotFound.visibility = visibilityError
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
            viewModel.userLocationVM.observeUserLocation()
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