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
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.RotationType
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import android.view.ViewGroup
import android.widget.EditText


class MapsFragment: BaseFragment<FragmentMapsBinding>(FragmentMapsBinding::inflate), UserLocationObjectListener {

    companion object {
        private const val TAG = "MapsFragment"
    }

    private val viewModel by sharedViewModel<MapsVM>()

    private lateinit var userLocationLayer: UserLocationLayer

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



    override fun onStop() {
        super.onStop()
        binding.mapview.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun onStart() {
        super.onStart()
        binding.mapview.onStart()
        MapKitFactory.getInstance().onStart()
    }

    private fun initViews() {
        

        val locationManager = MapKitFactory.getInstance().createLocationManager()
        locationManager.requestSingleUpdate(object : LocationListener {
            override fun onLocationStatusUpdated(p0: LocationStatus) {
                Log.w(TAG, p0.toString())
            }
            override fun onLocationUpdated(p0: Location) {
                Log.w(TAG, "lat=${p0.position.latitude} " +
                        "lon=${p0.position.longitude}")
            }
        })

        binding.searchView.setOnClickListener {
            val action = MapsFragmentDirections.actionMapsFragmentToSearchFragment()
            findNavController().navigate(action)
        }

        observe()
    }

    private fun observe() {
        viewModel.query.observe(viewLifecycleOwner) {
            Log.w(TAG, "maps fragment query updated")
            binding.searchView.setQuery(it, false)
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

        viewModel.searchCenter = userLocationView.arrow.geometry
    }

    override fun onObjectRemoved(p0: UserLocationView) {

    }

    override fun onObjectUpdated(userLocationView: UserLocationView, p1: ObjectEvent) {}
}