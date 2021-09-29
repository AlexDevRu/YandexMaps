package com.example.yandexmaps.ui.fragments.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.example.yandexmaps.R
import com.example.yandexmaps.args.toArg
import com.example.yandexmaps.databinding.FragmentMapsBinding
import com.example.yandexmaps.ui.fragments.base.BaseFragment
import com.example.yandexmaps.ui.helpers.DirectionHelper
import com.example.yandexmaps.ui.helpers.PanoramaHelper
import com.example.yandexmaps.ui.helpers.TrafficHelper
import com.example.yandexmaps.ui.helpers.UserLocationHelper
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.driving.*
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
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class MapsFragment: BaseFragment<FragmentMapsBinding>(FragmentMapsBinding::inflate) {

    companion object {
        private const val TAG = "MapsFragment"
    }

    private val viewModel by sharedViewModel<MapsVM>()

    private val cameraListener = CameraListener { _, cameraPosition, _, _ ->
        userLocationHelper.resetAnchor()
        Log.d(TAG, "camera position")
        viewModel.cameraPosition = cameraPosition
    }

    private val inputListener = object: InputListener {
        override fun onMapTap(p0: Map, p1: Point) {
            binding.mapview.map.deselectGeoObject()
            viewModel.selectedGeoObject.value = null
        }

        override fun onMapLongTap(p0: Map, p1: Point) {
            TODO("Not yet implemented")
        }
    }

    private val geoObjectTapListener = GeoObjectTapListener {
        viewModel.selectedGeoObject.value = it.geoObject

        val selectionMetadata = it.geoObject
            .metadataContainer
            .getItem(GeoObjectSelectionMetadata::class.java)

        val point = it.geoObject.geometry.firstOrNull()?.point
        Log.w(TAG, "selected point=${point?.latitude},${point?.longitude}; markerMode=${viewModel.markerMode}")

        if(viewModel.markerMode == MARKER_MODE.ORIGIN) {
            viewModel.origin.value = point
        } else if(viewModel.markerMode == MARKER_MODE.DESTINATION) {
            viewModel.destination.value = point
        }

        if (selectionMetadata != null) {
            binding.mapview.map.selectGeoObject(selectionMetadata.id, selectionMetadata.layerId)
        }

        selectionMetadata != null
    }

    private val locationUpdateListener = object : LocationListener {
        override fun onLocationStatusUpdated(p0: LocationStatus) {
            Log.w(TAG, p0.toString())
        }
        override fun onLocationUpdated(location: Location) {
            val lat = location.position.latitude
            val lng = location.position.longitude

            Log.w(TAG, "lat=${lat} " + "lon=${lng}")
            viewModel.userLocation = location.position
        }
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

        val locationManager = MapKitFactory.getInstance().createLocationManager()
        locationManager.subscribeForLocationUpdates(0.0, 3000, 2.0, true, FilteringMode.ON, locationUpdateListener)

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

        binding.directionsButton.setOnClickListener {
            val action = MapsFragmentDirections.actionMapsFragmentToDirectionsFragment()
            findNavController().navigate(action)
        }

        observe()
    }

    private fun observe() {
        viewModel.query.observe(viewLifecycleOwner) {
            Log.w(TAG, "maps fragment query updated")
        }

        viewModel.searchResponse.observe(viewLifecycleOwner) {

            searchCollection.clear()

            if(it == null) {
                binding.searchInfoContainer.visibility = View.GONE
                return@observe
            }

            binding.searchInfoContainer.visibility = View.VISIBLE

            val response = it.response

            binding.searchQuery.text = response.metadata.requestText

            var i = 0

            for (searchResult in response.collection.children) {
                if(i >= it.showResults) break
                val resultLocation = searchResult.obj?.geometry?.get(0)?.point
                if (resultLocation != null) {
                    searchCollection.addPlacemark(
                        resultLocation,
                        ImageProvider.fromResource(requireContext(), R.drawable.search_result)
                    )
                    ++i
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

        viewModel.origin.observe(viewLifecycleOwner) {
            originCollection.clear()
            if(it != null) {
                Log.w(TAG, "origin set $it")
                originCollection.addPlacemark(it, ImageProvider.fromResource(requireContext(), R.drawable.origin))
                buildDirection()
            }
        }
        viewModel.destination.observe(viewLifecycleOwner) {
            destinationCollection.clear()
            if(it != null) {
                Log.w(TAG, "destination set $it")
                destinationCollection.addPlacemark(it, ImageProvider.fromResource(requireContext(), R.drawable.destination))
                buildDirection()
            }
        }
    }

    private fun buildDirection() {
        val originPoint = viewModel.origin.value
        val destinationPoint = viewModel.destination.value

        if(originPoint != null && destinationPoint != null) {
            directionHelper.submitRequest(originPoint, destinationPoint)
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