package com.example.yandexmaps.ui.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.yandexmaps.ui.fragments.main.DIRECTION_ACTION

data class DirectionOptionUIModel(
    @DrawableRes val iconRes: Int,
    @StringRes val textRes: Int,
    val action: DIRECTION_ACTION
)