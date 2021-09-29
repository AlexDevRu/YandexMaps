package com.example.yandexmaps.ui.fragments.direction

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.yandexmaps.databinding.FragmentDirectionsBinding
import com.example.yandexmaps.ui.fragments.base.BaseFragment
import com.example.yandexmaps.ui.fragments.main.MARKER_MODE
import com.example.yandexmaps.ui.fragments.main.MapsVM
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class DirectionsFragment(): BaseFragment<FragmentDirectionsBinding>(FragmentDirectionsBinding::inflate) {

    companion object {
        private const val TAG = "DirectionsFragment"
    }

    private val mapsVM by sharedViewModel<MapsVM>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        binding.origin.setOnClickListener {
            Log.d(TAG, "origin")
            mapsVM.markerMode = MARKER_MODE.ORIGIN
            goToMaps()
        }
        binding.destination.setOnClickListener {
            mapsVM.markerMode = MARKER_MODE.DESTINATION
            goToMaps()
        }

        observe()
    }

    private fun goToMaps() {
        val action = DirectionsFragmentDirections.actionDirectionsFragmentToMapsFragment()
        findNavController().navigate(action)
    }

    private fun observe() {
        mapsVM.origin.observe(viewLifecycleOwner) {
            binding.origin.text = it?.toString() ?: "Choose origin"
        }

        mapsVM.destination.observe(viewLifecycleOwner) {
            binding.destination.text = it?.toString() ?: "Choose destination"
        }
    }
}