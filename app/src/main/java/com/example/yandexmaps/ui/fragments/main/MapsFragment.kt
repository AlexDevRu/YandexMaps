package com.example.yandexmaps.ui.fragments.main

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.example.yandexmaps.R
import com.example.yandexmaps.databinding.FragmentMapsBinding
import com.example.yandexmaps.ui.fragments.base.BaseFragment
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.location.FilteringMode
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.logo.Alignment
import com.yandex.mapkit.logo.HorizontalAlignment
import com.yandex.mapkit.logo.VerticalAlignment
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class MapsFragment: BaseFragment<FragmentMapsBinding>(FragmentMapsBinding::inflate), UserLocationObjectListener {

    companion object {
        private const val TAG = "MapsFragment"
    }

    private val viewModel by sharedViewModel<MapsVM>()

    private lateinit var userLocationLayer: UserLocationLayer

    private val cameraListener = CameraListener { _, _, _, _ ->
        userLocationLayer.resetAnchor()
        Log.d(TAG, "camera position")
    }

    private val inputListener = object: InputListener {
        override fun onMapTap(map: Map, point: Point) {
            binding.mapview.map.deselectGeoObject()
        }

        override fun onMapLongTap(map: Map, point: Point) {

        }
    }

    private val geoObjectTapListener = GeoObjectTapListener {
        val selectionMetadata = it.geoObject
            .metadataContainer
            .getItem(GeoObjectSelectionMetadata::class.java)

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestLocationPermission()

        val mapKit = MapKitFactory.getInstance()
        userLocationLayer = mapKit.createUserLocationLayer(binding.mapview.mapWindow)
        userLocationLayer.isVisible = true
        userLocationLayer.isHeadingEnabled = true
        userLocationLayer.isAutoZoomEnabled = true

        userLocationLayer.setObjectListener(this)
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

        /*if(viewModel.cameraPosition != null)
            binding.mapview.map.move(viewModel.cameraPosition!!)*/

        binding.mapview.map.addCameraListener(cameraListener)

        binding.myLocationButton.setOnClickListener {

            if(userLocationLayer.cameraPosition() == null) return@setOnClickListener

            binding.mapview.map.move(
                userLocationLayer.cameraPosition()!!,
                Animation(Animation.Type.SMOOTH, 2f),
                null
            )
        }

        observe()
    }

    private fun observe() {
        viewModel.query.observe(viewLifecycleOwner) {
            Log.w(TAG, "maps fragment query updated")
        }

        viewModel.searchResponse.observe(viewLifecycleOwner) {

            val mapObjects = binding.mapview.map.mapObjects
            mapObjects.clear()

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
                    mapObjects.addPlacemark(
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

    override fun onObjectAdded(userLocationView: UserLocationView) {
        if(viewModel.userAdded) return

        viewModel.userAdded = true

        Log.w(TAG, "onObjectAdded")

        val width = binding.mapview.width()
        val height = binding.mapview.height()

        userLocationLayer.setAnchor(
            PointF((width * 0.5).toFloat(), (height * 0.5).toFloat()),
            PointF((width * 0.5).toFloat(), (height * 0.83).toFloat())
        )

        userLocationView.arrow.setIcon(
            ImageProvider.fromResource(
                requireContext(), R.drawable.user_arrow
            )
        )

        val pinIcon = userLocationView.pin.useCompositeIcon()

        pinIcon.setIcon(
            "icon",
            ImageProvider.fromResource(requireContext(), R.drawable.icon),
            IconStyle().setAnchor(PointF(0f, 0f))
                .setRotationType(RotationType.ROTATE)
                .setZIndex(0f)
                .setScale(1f)
        )

        pinIcon.setIcon(
            "pin",
            ImageProvider.fromResource(requireContext(), R.drawable.search_result),
            IconStyle().setAnchor(PointF(0.5f, 0.5f))
                .setRotationType(RotationType.ROTATE)
                .setZIndex(1f)
                .setScale(0.5f)
        )

        viewModel.userLocation = userLocationView.pin.geometry
    }

    override fun onObjectRemoved(p0: UserLocationView) {

    }

    override fun onObjectUpdated(userLocationView: UserLocationView, p1: ObjectEvent) {

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