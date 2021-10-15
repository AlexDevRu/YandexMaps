package com.example.yandexmaps.utils

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.example.yandexmaps.R
import com.yandex.runtime.Error
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError


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


    fun getErrorMessage(error: Error) = when(error) {
        is RemoteError -> R.string.remote_error_message
        is NetworkError -> R.string.network_error_message
        else -> R.string.unknown_error_message
    }
}

fun View.hideKeyBoard() {
    val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
    imm?.hideSoftInputFromWindow(windowToken, 0)
}