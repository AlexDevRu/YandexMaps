package com.example.yandexmaps.ui.fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.yandexmaps.databinding.ViewholderSuggestionBinding
import com.yandex.mapkit.search.SuggestItem

class SuggestionsAdapter(private val clickHandler: (SuggestItem) -> Unit)
    : ListAdapter<SuggestItem, SuggestionsAdapter.SuggestionViewHolder>(SuggestionDiffUtil()){

    inner class SuggestionViewHolder(private val binding: ViewholderSuggestionBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(suggestion: SuggestItem) {
            binding.suggestionTextView.text = suggestion.displayText
            binding.root.setOnClickListener {
                clickHandler(suggestion)
            }
        }
    }

    companion object {
        class SuggestionDiffUtil : DiffUtil.ItemCallback<SuggestItem>() {
            override fun areItemsTheSame(oldItem: SuggestItem, newItem: SuggestItem): Boolean {
                return false
            }

            override fun areContentsTheSame(oldItem: SuggestItem, newItem: SuggestItem): Boolean {
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