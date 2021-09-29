package com.example.yandexmaps.ui.fragments.main

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.example.yandexmaps.R
import com.example.yandexmaps.args.toArg
import com.example.yandexmaps.databinding.FragmentMapsBinding
import com.example.yandexmaps.ui.fragments.base.BaseFragment
import com.example.yandexmaps.ui.helpers.DirectionHelper
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.*
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
import com.yandex.mapkit.places.PlacesFactory
import com.yandex.mapkit.places.panorama.PanoramaService
import com.yandex.mapkit.traffic.TrafficColor
import com.yandex.mapkit.traffic.TrafficLayer
import com.yandex.mapkit.traffic.TrafficLevel
import com.yandex.mapkit.traffic.TrafficListener
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.ArrayList


class MapsFragment: BaseFragment<FragmentMapsBinding>(FragmentMapsBinding::inflate), UserLocationObjectListener {

    companion object {
        private const val TAG = "MapsFragment"
    }

    private val viewModel by sharedViewModel<MapsVM>()

    private lateinit var userLocationLayer: UserLocationLayer

    private lateinit var panoramaService: PanoramaService



    private val cameraListener = CameraListener { _, cameraPosition, _, _ ->
        userLocationLayer.resetAnchor()
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

    private val panoramaListener = object: PanoramaService.SearchListener {
        override fun onPanoramaSearchResult(p0: String) {
            binding.panoramaButton.visibility = View.VISIBLE
        }

        override fun onPanoramaSearchError(p0: Error) {
            binding.panoramaButton.visibility = View.GONE
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


    private var trafficLevel: TrafficLevel? = null

    private enum class TrafficFreshness {
        Loading, OK, Expired
    }

    private var trafficFreshness: TrafficFreshness? = null

    private lateinit var traffic: TrafficLayer

    private val trafficListener = object: TrafficListener {
        override fun onTrafficChanged(tl: TrafficLevel?) {
            trafficLevel = tl
            trafficFreshness = TrafficFreshness.OK
            updateLevel()
        }

        override fun onTrafficLoading() {
            trafficLevel = null
            trafficFreshness = TrafficFreshness.Loading
            updateLevel()
        }

        override fun onTrafficExpired() {
            trafficLevel = null
            trafficFreshness = TrafficFreshness.Expired
            updateLevel()
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapKit = MapKitFactory.getInstance()

        panoramaService = PlacesFactory.getInstance().createPanoramaService()


        searchCollection = binding.mapview.map.mapObjects.addCollection()
        originCollection = binding.mapview.map.mapObjects.addCollection()
        destinationCollection = binding.mapview.map.mapObjects.addCollection()

        directionHelper = DirectionHelper(binding.mapview)

        requestLocationPermission()

        userLocationLayer = mapKit.createUserLocationLayer(binding.mapview.mapWindow)
        userLocationLayer.isVisible = true
        userLocationLayer.isHeadingEnabled = true
        userLocationLayer.isAutoZoomEnabled = true

        userLocationLayer.setObjectListener(this)


        traffic = MapKitFactory.getInstance().createTrafficLayer(binding.mapview.mapWindow)
        traffic.isTrafficVisible = true
        traffic.addTrafficListener(trafficListener)
        updateLevel()

        if(viewModel.cameraPosition != null) {
            binding.mapview.map.move(viewModel.cameraPosition!!)
        }
    }

    private fun updateLevel() {
        val iconId: Int
        var level: String? = ""
        if (!traffic.isTrafficVisible) {
            iconId = R.drawable.icon_traffic_light_dark
        } else if (trafficFreshness == TrafficFreshness.Loading) {
            iconId = R.drawable.icon_traffic_light_violet
        } else if (trafficFreshness == TrafficFreshness.Expired) {
            iconId = R.drawable.icon_traffic_light_blue
        } else if (trafficLevel == null) {  // state is fresh but region has no data
            iconId = R.drawable.icon_traffic_light_grey
        } else {
            iconId = when (trafficLevel?.color) {
                TrafficColor.RED -> R.drawable.icon_traffic_light_red
                TrafficColor.GREEN -> R.drawable.icon_traffic_light_green
                TrafficColor.YELLOW -> R.drawable.icon_traffic_light_yellow
                else -> R.drawable.icon_traffic_light_grey
            }
            level = trafficLevel?.level.toString()
        }
        binding.trafficView.trafficLight.setImageBitmap(BitmapFactory.decodeResource(resources, iconId))
        binding.trafficView.trafficLightText.text = level
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

        binding.trafficView.trafficLight.setOnClickListener {
            traffic.isTrafficVisible = !traffic.isTrafficVisible
            updateLevel()
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
                    panoramaService.findNearest(point, panoramaListener)
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