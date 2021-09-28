package com.example.yandexmaps.ui.models

import com.yandex.mapkit.search.Response

data class SearchResponseModel(
    val response: Response,
    val showResults: Int = 1
)