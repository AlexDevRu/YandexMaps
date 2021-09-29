package com.example.yandexmaps

import android.app.Application
import com.example.yandexmaps.di.viewModelModule
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.places.PlacesFactory
import com.yandex.mapkit.transport.TransportFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MapsApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey("ca1622dc-b730-49b7-ba6a-dbcecaa88625")
        MapKitFactory.initialize(this)
        TransportFactory.initialize(this)
        PlacesFactory.initialize(this)
        DirectionsFactory.initialize(this)

        startKoin {
            androidContext(this@MapsApplication)
            modules(viewModelModule)
        }
    }
}