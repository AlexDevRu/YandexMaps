package com.example.yandexmaps.ui.fragments.place

import android.os.Bundle
import android.util.Log
import android.view.View
import com.example.yandexmaps.databinding.FragmentPlaceInfoBinding
import com.example.yandexmaps.ui.fragments.base.BaseFragment
import com.example.yandexmaps.ui.fragments.main.MapsFragment
import com.example.yandexmaps.ui.fragments.main.MapsVM
import com.example.yandexmaps.ui.fragments.main.Result
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class PlaceInfoFragment: BaseFragment<FragmentPlaceInfoBinding>(FragmentPlaceInfoBinding::inflate) {

    companion object {
        private const val TAG = "PlaceInfoFragment"
    }

    private val mapsVM by sharedViewModel<MapsVM>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe()
    }

    private fun observe() {
        mapsVM.selectedGeoObjectMetadata.observe(viewLifecycleOwner) metadataObserve@ {
            when(it) {
                is Result.Success -> {
                    binding.placeName.text = it.value.generalMetadata.name
                    binding.placeAddress.text = it.value.generalMetadata.address.formattedAddress
                    binding.phoneNumber.text = it.value.generalMetadata.phones.map { it.formattedNumber }.joinToString(", ")

                    Log.d(TAG, "panoramas ${it.value.panoramasMetadata}")
                    Log.d(TAG, "images ${it.value.imagesMetadata}")

                    Log.d(TAG, "selected working hours ${it.value.generalMetadata?.workingHours?.availabilities?.firstOrNull()?.days} ${it.value.generalMetadata?.workingHours?.availabilities?.firstOrNull()?.timeRanges?.firstOrNull()?.from}")
                }
            }
        }
    }
}