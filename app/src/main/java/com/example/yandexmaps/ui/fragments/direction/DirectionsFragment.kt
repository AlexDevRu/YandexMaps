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
        updatePoints()

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
        findNavController().navigateUp()
    }

    private fun goToDirectionOptions() {
        /*val action = DirectionsFragmentDirections.actionDirectionsFragmentToSelectDirectionInputDialog()
        findNavController().navigate(action)*/
    }

    private fun observe() {
        mapsVM.originAddress.observe(viewLifecycleOwner) {
            if(mapsVM.directionAction.value == DIRECTION_ACTION.CHOOSE_ON_MAP && mapsVM.bindedMarkerType == MARKER_MODE.ORIGIN)
                binding.origin.text = it ?: resources.getString(R.string.choose_origin)
        }

        mapsVM.destinationAddress.observe(viewLifecycleOwner) {
            if(mapsVM.directionAction.value == DIRECTION_ACTION.CHOOSE_ON_MAP && mapsVM.bindedMarkerType == MARKER_MODE.DESTINATION)
                binding.destination.text = it ?: resources.getString(R.string.choose_destination)
        }

        mapsVM.directionAction.observe(viewLifecycleOwner) {
            if(it == DIRECTION_ACTION.CHOOSE_ON_MAP) {
                goToMaps()
            } else updatePoints()
        }
    }

    private fun updatePoints() {
        if(mapsVM.directionAction.value == DIRECTION_ACTION.BIND_MY_LOCATION) {
            if(mapsVM.bindedMarkerType == MARKER_MODE.ORIGIN)
                binding.origin.text = resources.getString(R.string.my_location)
            else if(mapsVM.bindedMarkerType == MARKER_MODE.DESTINATION)
                binding.destination.text = resources.getString(R.string.my_location)
        }
    }
}