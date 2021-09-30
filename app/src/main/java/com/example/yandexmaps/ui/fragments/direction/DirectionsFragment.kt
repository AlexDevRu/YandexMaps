package com.example.yandexmaps.ui.fragments.direction

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.yandexmaps.R
import com.example.yandexmaps.databinding.FragmentDirectionsBinding
import com.example.yandexmaps.ui.fragments.base.BaseFragment
import com.example.yandexmaps.ui.fragments.main.DIRECTION_ACTION
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
            mapsVM.markerMode.value = MARKER_MODE.ORIGIN
            goToDirectionOptions()
        }
        binding.destination.setOnClickListener {
            mapsVM.markerMode.value = MARKER_MODE.DESTINATION
            goToDirectionOptions()
        }

        observe()
    }

    private fun goToMaps() {
        if(findNavController().currentDestination?.id == R.id.selectDirectionInputDialog) {
            findNavController().navigateUp()
        }
        findNavController().navigateUp()
    }

    private fun goToDirectionOptions() {
        if(findNavController().currentDestination?.id == R.id.selectDirectionInputDialog) {
            findNavController().navigateUp()
        }
        val action = DirectionsFragmentDirections.actionDirectionsFragmentToSelectDirectionInputDialog()
        findNavController().navigate(action)
    }

    private fun observe() {
        mapsVM.originAddress.observe(viewLifecycleOwner) {
            binding.origin.text = it ?: "Choose origin"
        }

        mapsVM.destinationAddress.observe(viewLifecycleOwner) {
            binding.destination.text = it ?: "Choose destination"
        }

        mapsVM.directionAction.observe(viewLifecycleOwner) {
            Log.e(TAG, "currentDestination ${findNavController().currentDestination}")
            if(it == DIRECTION_ACTION.CHOOSE_ON_MAP) {
                goToMaps()
            }
        }
    }
}