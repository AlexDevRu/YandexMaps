package com.example.yandexmaps.utils.extensions

import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.uri.UriObjectMetadata

fun GeoObject.uri(): String? {
    val metadata = metadataContainer.getItem(UriObjectMetadata::class.java)
    return metadata.uris.firstOrNull()?.value
}