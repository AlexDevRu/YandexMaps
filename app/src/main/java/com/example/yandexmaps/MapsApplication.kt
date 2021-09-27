package com.example.yandexmaps

import android.app.Application
import com.example.yandexmaps.di.viewModelModule
import com.yandex.mapkit.MapKitFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MapsApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey("ca1622dc-b730-49b7-ba6a-dbcecaa88625")
        MapKitFactory.initialize(this)

        startKoin {
            androidContext(this@MapsApplication)
            modules(viewModelModule)
        }
    }
}