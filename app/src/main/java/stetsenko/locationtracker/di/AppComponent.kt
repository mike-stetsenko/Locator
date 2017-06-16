package stetsenko.locationtracker.di

import dagger.Component
import stetsenko.locationtracker.LocationKeeper
import stetsenko.locationtracker.LocationLoggerService
import stetsenko.locationtracker.MapsActivity
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(AppModule::class))
interface AppComponent {

    val locationKeeper: LocationKeeper

    fun inject(locationLoggerService: LocationLoggerService)
    fun inject(locationLoggerService: LocationKeeper)
    fun inject(mapsActivity: MapsActivity)
}
