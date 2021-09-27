package com.example.yandexmaps.ui.fragments.main

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.yandexmaps.R
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.*
import com.yandex.runtime.Error
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError

class MapsVM: ViewModel() {

    var query = MutableLiveData<String>()

    var searchCenter = Point()

    var suggestionsList = MutableLiveData<List<String?>>()
}