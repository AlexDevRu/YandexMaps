package com.example.yandexmaps.utils

import android.content.Context
import com.example.yandexmaps.R

object Utils {

    private fun f(n: Int, u: String) = if(n > 0) "${n} ${u} " else ""

    fun formatDistance(context: Context, metres: Double): String {
        val km = (metres / 1000).toInt()
        val restMetres = (metres % 1000).toInt()

        val kmUnit = context.getString(R.string.km)
        val mUnit = context.getString(R.string.m)

        return f(km, kmUnit) + f(restMetres, mUnit)
    }

    fun formatDuration(context: Context, seconds: Double): String {
        val h = (seconds / 3600).toInt()
        val m = ((seconds % 3600) / 60).toInt()
        val s = (seconds - h * 3600 - m * 60).toInt()

        val hoursUnit = context.getString(R.string.hours)
        val minutesUnit = context.getString(R.string.minutes)
        val secondsUnit = context.getString(R.string.seconds)

        return f(h, hoursUnit) + f(m, minutesUnit) + f(s, secondsUnit)
    }
}