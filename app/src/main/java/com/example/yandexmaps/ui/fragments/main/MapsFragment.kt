package com.example.yandexmaps.ui.fragments.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.yandexmaps.R
import com.example.yandexmaps.args.toArg
import com.example.yandexmaps.databinding.FragmentMapsBinding
import com.example.yandexmaps.ui.fragments.base.BaseFragment
import com.example.yandexmaps.ui.fragments.search.SearchFragmentDirections
import com.example.yandexmaps.ui.helpers.DirectionHelper
import com.example.yandexmaps.ui.helpers.PanoramaHelper
import com.example.yandexmaps.ui.helpers.TrafficHelper
import com.example.yandexmaps.ui.helpers.UserLocationHelper
import com.example.yandexmaps.ui.models.SearchResponseModel
import com.yandex.mapkit.Animation
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.driving.*
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.location.FilteringMode
import com.yandex.mapkit.logo.Alignment
import com.yandex.mapkit.logo.HorizontalAlignment
import com.yandex.mapkit.logo.VerticalAlignment
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
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
        if(viewModel.markerMode.value == MARKER_MODE.ORIGIN) {
            viewModel.origin.value = point
        } else if(viewModel.markerMode.value == MARKER_MODE.DESTINATION) {
            viewModel.destination.value = point
        }
    }

    private val geoObjectTapListener = GeoObjectTapListener {
        viewModel.selectedGeoObject.value = it.geoObject

        val selectionMetadata = it.geoObject
            .metadataContainer
            .getItem(GeoObjectSelectionMetadata::class.java)

        val point = it.geoObject.geometry.firstOrNull()?.point

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

            Log.w(TAG, "onObjectAdded")

            viewModel.userLocation = it.pin.geometry

            return@UserLocationHelper true
        }

        requestLocationPermission()

        if(viewModel.cameraPosition != null) {
            binding.mapview.map.move(viewModel.cameraPosition!!)
        }
    }


    private fun initViews() {
        binding.mapview.map.logo.setAlignment(Alignment(HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM))

        binding.searchButton.setOnClickListener {
            val action = MapsFragmentDirections.actionMapsFragmentToSearchFragment()
            findNavController().navigate(action)
        }

        binding.clearSearchButton.setOnClickListener {
            viewModel.searchResponse.value = null
        }

        Log.d(TAG, "init listeners")

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

        binding.panoramaButton.setOnClickListener {
            val point = viewModel.selectedGeoObject.value?.geometry?.getOrNull(0)?.point

            if(point != null) {
                val action = MapsFragmentDirections.actionMapsFragmentToPanoramaFragment(point.toArg())
                findNavController().navigate(action)
            }
        }

        binding.directionLayout.origin.setOnClickListener {
            Log.e(TAG, "current destination ${findNavController().currentDestination}")
            viewModel.markerMode.value = MARKER_MODE.ORIGIN
            val action = MapsFragmentDirections.actionMapsFragmentToSelectDirectionInputDialog()
            findNavController().navigate(action)
        }

        binding.directionLayout.destination.setOnClickListener {
            Log.e(TAG, "current destination ${findNavController().currentDestination}")
            viewModel.markerMode.value = MARKER_MODE.DESTINATION
            val action = MapsFragmentDirections.actionMapsFragmentToSelectDirectionInputDialog()
            findNavController().navigate(action)
        }

        observe()
    }

    private val queryObserver = Observer<String> {
        Log.w(TAG, "maps fragment query updated")
    }
    private val searchResponseObserver = Observer<SearchResponseModel?> {
        searchCollection.clear()

        if(it == null) {
            binding.searchInfoContainer.visibility = View.GONE
            return@Observer
        }

        binding.searchInfoContainer.visibility = View.VISIBLE

        val response = it.response

        binding.searchQuery.text = response.metadata.requestText

        var i = 0

        for (searchResult in response.collection.children) {
            val resultLocation = searchResult.obj?.geometry?.get(0)?.point
            if (resultLocation != null) {
                if(i++ >= it.showResults) break
                when (viewModel.markerMode.value) {
                    MARKER_MODE.ORIGIN -> {
                        viewModel.origin.value = resultLocation
                    }
                    MARKER_MODE.DESTINATION -> {
                        viewModel.destination.value = resultLocation
                    }
                    else -> {
                        searchCollection.addPlacemark(
                            resultLocation,
                            ImageProvider.fromResource(requireContext(), R.drawable.search_result)
                        )
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

    private val selectedGeoObjectObserver = Observer<GeoObject?> {
        if(it != null && viewModel.markerMode.value == MARKER_MODE.PLACE) {
            val point = viewModel.selectedGeoObject.value?.geometry?.getOrNull(0)?.point
            if(point != null)
                panoramaHelper.findNearest(point, {
                    binding.panoramaButton.visibility = View.VISIBLE
                }, {
                    binding.panoramaButton.visibility = View.GONE
                })
            else
                binding.panoramaButton.visibility = View.GONE
        } else {
            binding.panoramaButton.visibility = View.GONE
        }
    }

    private val originObserver = Observer<Point?> {
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
            viewModel.originAddress.value = null
        }
    }

    private val destinationObserver = Observer<Point?> {
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
            viewModel.destinationAddress.value = null
        }
    }

    private fun observe() {
        viewModel.query.observe(viewLifecycleOwner, queryObserver)

        viewModel.searchResponse.observe(viewLifecycleOwner, searchResponseObserver)

        viewModel.selectedGeoObject.observe(viewLifecycleOwner, selectedGeoObjectObserver)

        viewModel.origin.observe(viewLifecycleOwner, originObserver)

        viewModel.destination.observe(viewLifecycleOwner, destinationObserver)

        viewModel.markerMode.observe(viewLifecycleOwner) {
            Log.d(TAG, "marker mode observer ${it}")
            searchCollection.isVisible = it == MARKER_MODE.PLACE
            originCollection.isVisible = it != MARKER_MODE.PLACE
            destinationCollection.isVisible = it != MARKER_MODE.PLACE
            directionHelper.updateVisibility(it != MARKER_MODE.PLACE)
        }

        viewModel.originAddress.observe(viewLifecycleOwner) {
            Log.d(TAG, "originAddress")
            if(viewModel.markerMode.value == MARKER_MODE.ORIGIN)
                binding.directionLayout.origin.text = it
        }

        viewModel.destinationAddress.observe(viewLifecycleOwner) {
            Log.d(TAG, "destinationAddress")
            if(viewModel.markerMode.value == MARKER_MODE.DESTINATION)
                binding.directionLayout.destination.text = it
        }
    }

    private fun searchByPoint(point: Point, onResponse: (Response) -> Unit, onFailure: (Error) -> Unit) {
        searchSession = searchManager.submit(
            point,
            11,
            SearchOptions(),
            object: Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    onResponse(response)
                }

                override fun onSearchError(error: Error) {
                    showSnackBar(error.toString())
                    onFailure(error)
                }
            }
        )
    }

    private fun buildDirection() {
        val originPoint = viewModel.origin.value
        val destinationPoint = viewModel.destination.value

        if(originPoint != null && destinationPoint != null) {
            directionHelper.submitRequest(originPoint, destinationPoint) {
                it.forEach { route ->
                    Log.w(TAG, "route ${route}")
                    Log.w(TAG, "route.metadata.weight ${route.metadata.weight.distance.text} ${route.metadata.weight.timeWithTraffic.text}")
                }
            }
        }
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
            locationManager.subscribeForLocationUpdates(0.0, 3000, 2.0, true, FilteringMode.ON, viewModel.locationUpdateListener)
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