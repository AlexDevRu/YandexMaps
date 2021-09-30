package com.example.yandexmaps.ui.fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.yandexmaps.databinding.ViewholderDirectionOptionBinding
import com.example.yandexmaps.ui.models.DirectionOptionUIModel


class DirectionOptionsAdapter(private val onClick: (DirectionOptionUIModel) -> Unit)
    : ListAdapter<DirectionOptionUIModel, DirectionOptionsAdapter.DirectionOptionViewHolder>(DirectionOptionUIModelDiffUtil()) {

    inner class DirectionOptionViewHolder(private val binding: ViewholderDirectionOptionBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(option: DirectionOptionUIModel) {
            binding.optionIcon.setImageResource(option.iconRes)
            binding.optionText.text = itemView.resources.getString(option.textRes)
            binding.root.setOnClickListener {
                onClick(option)
            }
        }
    }

    companion object {
        class DirectionOptionUIModelDiffUtil: DiffUtil.ItemCallback<DirectionOptionUIModel>() {
            override fun areItemsTheSame(
                oldItem: DirectionOptionUIModel,
                newItem: DirectionOptionUIModel
            ): Boolean {
                return false
            }

            override fun areContentsTheSame(
                oldItem: DirectionOptionUIModel,
                newItem: DirectionOptionUIModel
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectionOptionViewHolder {
        val binding = ViewholderDirectionOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return DirectionOptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DirectionOptionViewHolder, position: Int) {
        getItem(position).let { holder.bind(it) }
    }
}