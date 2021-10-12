package com.example.yandexmaps.ui.fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.yandexmaps.R
import com.example.yandexmaps.databinding.ViewholderDrivingBinding
import com.yandex.mapkit.directions.driving.Action
import com.yandex.mapkit.directions.driving.DrivingSectionMetadata


class DrivingAdapter(private val onClick: (DrivingSectionMetadata) -> Unit)
    : ListAdapter<DrivingSectionMetadata, DrivingAdapter.DrivingSectionMetadataViewHolder>(DrivingSectionMetadataDiffUtil()) {


    inner class DrivingSectionMetadataViewHolder(private val binding: ViewholderDrivingBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(metadata: DrivingSectionMetadata, last: Boolean) {

            binding.distance.text = metadata.weight.distance.text
            binding.duration.text = metadata.weight.timeWithTraffic.text

            binding.textView.text = when(metadata.annotation.action) {
                Action.LEFT,
                Action.SLIGHT_LEFT,
                Action.UTURN_LEFT -> itemView.context.getString(R.string.driving_description, getString(R.string.left))

                Action.RIGHT,
                Action.SLIGHT_RIGHT,
                Action.UTURN_RIGHT -> itemView.context.getString(R.string.driving_description, getString(R.string.right))

                Action.EXIT_LEFT -> itemView.context.getString(R.string.exit_driving_description, getString(R.string.left))
                Action.EXIT_RIGHT -> itemView.context.getString(R.string.exit_driving_description, getString(R.string.right))
                Action.STRAIGHT -> itemView.context.getString(R.string.driving_description, getString(R.string.forward))
                Action.FORK_LEFT -> itemView.context.getString(R.string.fork_driving_description, getString(R.string.left))
                Action.FORK_RIGHT -> itemView.context.getString(R.string.fork_driving_description, getString(R.string.right))
                else -> metadata.annotation.action?.name
            }

            binding.icon.setImageResource(
                when(metadata.annotation.action) {
                    Action.LEFT,
                    Action.SLIGHT_LEFT,
                    Action.EXIT_LEFT,
                    Action.UTURN_LEFT -> R.drawable.ic_baseline_arrow_back_24

                    Action.RIGHT,
                    Action.SLIGHT_RIGHT,
                    Action.EXIT_RIGHT,
                    Action.UTURN_RIGHT -> R.drawable.ic_baseline_arrow_forward_24

                    Action.STRAIGHT -> R.drawable.ic_baseline_arrow_upward_24
                    Action.FORK_LEFT -> R.drawable.fork_right
                    Action.FORK_RIGHT -> R.drawable.fork_right
                    else -> R.drawable.ic_baseline_close_24
                }
            )

            if(metadata.annotation.action == Action.FORK_LEFT) {
                binding.icon.scaleX = -1f
            }

            if(last) {
                binding.root.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.white)
            } else {
                binding.root.background = ContextCompat.getDrawable(itemView.context, R.drawable.viewholder_driving_bg)
            }
        }

        private fun getString(@StringRes stringRes: Int) = itemView.context.getString(stringRes)
    }

    companion object {
        private const val TAG = "DrivingAdapter"

        class DrivingSectionMetadataDiffUtil: DiffUtil.ItemCallback<DrivingSectionMetadata>() {
            override fun areItemsTheSame(
                oldItem: DrivingSectionMetadata,
                newItem: DrivingSectionMetadata
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: DrivingSectionMetadata,
                newItem: DrivingSectionMetadata
            ): Boolean {
                return false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrivingSectionMetadataViewHolder {
        val binding = ViewholderDrivingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return DrivingSectionMetadataViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DrivingSectionMetadataViewHolder, position: Int) {
        getItem(position).let { holder.bind(it, position == (itemCount - 1)) }
    }
}