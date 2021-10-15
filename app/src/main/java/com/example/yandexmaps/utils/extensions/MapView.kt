package com.example.yandexmaps.utils.extensions

import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.mapview.MapView

fun MapView.visibleRegion() = VisibleRegionUtils.toPolygon(map.visibleRegion)