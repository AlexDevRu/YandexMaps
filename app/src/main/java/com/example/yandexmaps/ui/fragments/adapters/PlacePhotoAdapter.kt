package com.example.yandexmaps.ui.fragments.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.yandexmaps.databinding.ViewholderPhotoBinding
import com.example.yandexmaps.utils.extensions.load
import com.yandex.mapkit.search.BusinessPhotoObjectMetadata

class PlacePhotosAdapter(private val p: () -> Unit)
    : ListAdapter<BusinessPhotoObjectMetadata.Photo, PlacePhotosAdapter.PlacePhotoViewHolder>(PlacePhotosDiffUtil()) {

    inner class PlacePhotoViewHolder(private val binding: ViewholderPhotoBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(photo: BusinessPhotoObjectMetadata.Photo) {

            val uri = photo.links.firstOrNull()?.uri
            Log.d("asd", "photo uri $uri")
            binding.photo.load(uri)

            binding.photo.transitionName = uri

            binding.root.setOnClickListener {
                /*val extras = FragmentNavigatorExtras(
                    binding.photo to binding.photo.transitionName,
                )*/

                //val action = MainFragmentDirections.actionMainFragmentToFullPhotoFragment(photoUrl)
                //itemView.findNavController().navigate(action, extras)
            }
        }
    }

    companion object {
        class PlacePhotosDiffUtil(): DiffUtil.ItemCallback<BusinessPhotoObjectMetadata.Photo>() {
            override fun areItemsTheSame(oldItem: BusinessPhotoObjectMetadata.Photo, newItem: BusinessPhotoObjectMetadata.Photo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: BusinessPhotoObjectMetadata.Photo, newItem: BusinessPhotoObjectMetadata.Photo): Boolean {
                return false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlacePhotoViewHolder {
        val binding = ViewholderPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return PlacePhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlacePhotoViewHolder, position: Int) {
        getItem(position).let { holder.bind(it) }
    }
}