package com.example.yandexmaps.ui.models

import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.search.*

data class GeoObjectMetadataModel(
    val response: Response,
    val geoObject: GeoObject?,
    val panoramasMetadata: PanoramasObjectMetadata?,
    val imagesMetadata: BusinessImagesObjectMetadata?,
    val photosMetadata: BusinessPhotoObjectMetadata?,
    val generalMetadata: BusinessObjectMetadata,
)