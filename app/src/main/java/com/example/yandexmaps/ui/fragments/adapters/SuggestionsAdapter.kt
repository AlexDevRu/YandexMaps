package com.example.yandexmaps.ui.fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.yandexmaps.databinding.ViewholderSuggestionBinding

class SuggestionsAdapter(private val clickHandler: () -> Unit)
    : ListAdapter<String, SuggestionsAdapter.SuggestionViewHolder>(SuggestionDiffUtil()){

    inner class SuggestionViewHolder(private val binding: ViewholderSuggestionBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(suggestion: String) {
            binding.suggestionTextView.text = suggestion
            binding.root.setOnClickListener {
                clickHandler()
            }
        }
    }

    companion object {
        class SuggestionDiffUtil : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return false
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val binding = ViewholderSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return SuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        getItem(position).let { holder.bind(it) }
    }
}