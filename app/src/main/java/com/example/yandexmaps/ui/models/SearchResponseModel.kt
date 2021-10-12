package com.example.yandexmaps.ui.models

import com.yandex.mapkit.search.Response

data class SearchResponseModel(
    val response: Response,
    val searchText: String,
    val searchLayer: Boolean = false,
    val showResults: Int = 1
)