package com.example.yandexmaps.ui.fragments.panorama

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import com.example.yandexmaps.args.toModel
import com.example.yandexmaps.databinding.FragmentPanoramaBinding
import com.example.yandexmaps.ui.fragments.base.BaseFragment
import com.example.yandexmaps.utils.Utils
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.places.PlacesFactory
import com.yandex.mapkit.places.panorama.PanoramaService
import com.yandex.mapkit.places.panorama.PanoramaService.SearchSession
import com.yandex.runtime.Error

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
        Toast.makeText(requireContext(), Utils.getErrorMessage(error), Toast.LENGTH_SHORT).show()
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