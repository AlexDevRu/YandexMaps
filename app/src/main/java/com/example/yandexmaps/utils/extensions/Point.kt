package com.example.yandexmaps.utils.extensions

import com.yandex.mapkit.geometry.Point
import kotlin.math.*

fun Point.distance(to: Point): Double {
    val R = 6371 // Радиус Земли
    val latDistance = Math.toRadians(to.latitude - latitude)
    val lonDistance = Math.toRadians(to.longitude - longitude)
    val a = (sin(latDistance / 2) * sin(latDistance / 2)
            + (cos(Math.toRadians(latitude)) * cos(Math.toRadians(to.latitude))
            * sin(lonDistance / 2) * sin(lonDistance / 2)))
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    var distance = R * c * 1000 // Преобразование единиц в метры
    distance = distance.pow(2.0)
    return sqrt(distance)
}
