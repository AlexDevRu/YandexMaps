package com.example.yandexmaps.ui.fragments.panorama

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import com.example.yandexmaps.R
import com.example.yandexmaps.args.toModel
import com.example.yandexmaps.databinding.FragmentPanoramaBinding
import com.example.yandexmaps.ui.fragments.base.BaseFragment
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.places.PlacesFactory
import com.yandex.mapkit.places.panorama.NotFoundError
import com.yandex.mapkit.places.panorama.PanoramaService
import com.yandex.mapkit.places.panorama.PanoramaService.SearchSession
import com.yandex.runtime.Error
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError

class PanoramaFragment: BaseFragment<FragmentPanoramaBinding>(FragmentPanoramaBinding::inflate),
    PanoramaService.SearchListener {

    private val args by navArgs<PanoramaFragmentArgs>()

    private lateinit var panoramaService: PanoramaService
    private lateinit var searchSession: SearchSession

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        panoramaService = PlacesFactory.getInstance().createPanoramaService()
        searchSession = panoramaService.findNearest(args.point.toModel(), this)
    }

    override fun onPanoramaSearchResult(panoramaId: String) {
        binding.panoview.player.openPanorama(panoramaId)
        binding.panoview.player.enableMove()
        binding.panoview.player.enableRotation()
        binding.panoview.player.enableZoom()
        binding.panoview.player.enableMarkers()
    }

    override fun onPanoramaSearchError(error: Error) {
        var errorMessage = getString(R.string.unknown_error_message)
        when (error) {
            is NotFoundError -> {
                errorMessage = getString(R.string.not_found_error_message)
            }
            is RemoteError -> {
                errorMessage = getString(R.string.remote_error_message)
            }
            is NetworkError -> {
                errorMessage = getString(R.string.network_error_message)
            }
        }

        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
    }


    override fun onStop() {
        binding.panoview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.panoview.onStart()
    }
}