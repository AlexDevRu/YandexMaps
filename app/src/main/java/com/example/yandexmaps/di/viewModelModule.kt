package com.example.yandexmaps.di

import com.example.yandexmaps.ui.fragments.main.MapsVM
import com.example.yandexmaps.ui.fragments.search.SearchVM
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        MapsVM()
    }
    viewModel {
        SearchVM()
    }
}