package com.example.yandexmaps.ui.fragments.place

import android.os.Bundle
import android.util.Log
import android.view.View
import com.example.yandexmaps.databinding.FragmentPlacePhotosBinding
import com.example.yandexmaps.ui.fragments.adapters.PlacePhotosAdapter
import com.example.yandexmaps.ui.fragments.base.BaseFragment
import com.example.yandexmaps.ui.fragments.main.MapsVM
import com.example.yandexmaps.ui.fragments.main.Result
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class PlacePhotosFragment: BaseFragment<FragmentPlacePhotosBinding>(FragmentPlacePhotosBinding::inflate) {

    private val mapsVM by sharedViewModel<MapsVM>()

    private lateinit var adapter: PlacePhotosAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = PlacePhotosAdapter {

        }
        binding.placePhotosList.adapter = adapter
        observe()
    }

    private fun observe() {
        mapsVM.selectedGeoObjectMetadata.observe(viewLifecycleOwner) selectedObserve@ {
            when(it) {
                is Result.Success -> {
                    Log.d("asd", "eroyitoyitoyitoyiotyitoyiotyitoyitoyito ${it.value.photosMetadata?.photos}")
                    adapter.submitList(it.value.photosMetadata?.photos)
                }
            }
        }
    }
}