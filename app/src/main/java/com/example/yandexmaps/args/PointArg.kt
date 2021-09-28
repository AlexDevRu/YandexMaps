package com.example.yandexmaps.args

import android.os.Parcelable
import com.yandex.mapkit.geometry.Point
import kotlinx.parcelize.Parcelize

@Parcelize
data class PointArg(
    val latitude: Double,
    val longitude: Double
): Parcelable

fun Point.toArg(): PointArg {
    return PointArg(latitude, longitude)
}

fun PointArg.toModel(): Point {
    return Point(latitude, longitude)
}