package com.example.yandexmaps.ui.fragments.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.yandexmaps.R
import com.example.yandexmaps.databinding.ViewholderMasstransitBinding
import com.yandex.mapkit.transport.masstransit.Section

class MassTransitAdapter(private val onClick: (Section) -> Unit)
    : ListAdapter<Section, MassTransitAdapter.SectionViewHolder>(SectionDiffUtil()
) {

    inner class SectionViewHolder(private val binding: ViewholderMasstransitBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(section: Section, last: Boolean) {

            binding.fromStopName.text = section.stops.firstOrNull()?.stop?.name
            binding.toStopName.text = section.stops.lastOrNull()?.stop?.name

            Log.w(TAG, "mass transit lines ${section.metadata.data.transports?.map { it.line.name }}")
            Log.w(TAG, "mass transit veh types ${section.metadata.data.transports?.map { it.line.vehicleTypes }}")
            Log.w(TAG, "================================")

            val firstTransport = section.metadata.data.transports?.firstOrNull()

            if(firstTransport != null) {
                binding.transportNumber.text = firstTransport.line.name
                binding.transportIcon.setImageResource(
                    when (firstTransport.line.vehicleTypes.first()) {
                        "bus" -> {
                            R.drawable.bus
                        }
                        "tramway" -> {
                            R.drawable.tramway
                        }
                        "trolleybus" -> {
                            R.drawable.trolleybus
                        }
                        "minibus" -> {
                            R.drawable.minibus
                        }
                        "railway" -> {
                            R.drawable.train
                        }
                        "underground" -> R.drawable.metro
                        else -> R.drawable.ic_baseline_close_24
                    }
                )
                if(firstTransport.line.vehicleTypes.first() == "underground") {
                    binding.transportNumber.visibility = View.GONE
                    binding.transportIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.red))
                }
            } else {
                binding.transportNumber.visibility = View.GONE
                binding.transportIcon.setImageResource(R.drawable.walk)
                binding.fromLabel.text = "Distance"
                binding.toLabel.text = "Duration"
                binding.fromStopName.text = section.metadata.weight.walkingDistance.text
                binding.toStopName.text = section.metadata.weight.time.text
            }

            if(!last) {
                binding.root.background = ContextCompat.getDrawable(itemView.context, R.drawable.viewholder_driving_bg)
            }
        }
    }

    companion object {
        private const val TAG = "MassTransitAdapter"

        class SectionDiffUtil: DiffUtil.ItemCallback<Section>() {
            override fun areItemsTheSame(
                oldItem: Section,
                newItem: Section
            ): Boolean {
                return false
            }

            override fun areContentsTheSame(
                oldItem: Section,
                newItem: Section
            ): Boolean {
                return false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ViewholderMasstransitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return SectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        getItem(position).let { holder.bind(it, position == (itemCount - 1)) }
    }
}