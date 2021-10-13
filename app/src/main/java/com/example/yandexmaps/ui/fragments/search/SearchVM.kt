package com.example.yandexmaps.ui.fragments.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.yandex.mapkit.search.SuggestItem

class SearchVM: ViewModel() {
    val query = MutableLiveData<String>()
    val suggestionsList = MutableLiveData<List<SuggestItem?>>()
}