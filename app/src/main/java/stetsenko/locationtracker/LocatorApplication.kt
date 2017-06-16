package stetsenko.locationtracker

import android.app.Application
import stetsenko.locationtracker.di.AppComponent
import stetsenko.locationtracker.di.AppModule
import stetsenko.locationtracker.di.DaggerAppComponent
import timber.log.Timber
import timber.log.Timber.DebugTree




class LocatorApplication : Application() {

    lateinit var appComponent: AppComponent

    companion object {
        lateinit var instance: LocatorApplication
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        appComponent = buildComponent()

        Timber.plant(DebugTree())
    }

    fun buildComponent(): AppComponent {
        return DaggerAppComponent.builder()
                .appModule(AppModule(this))
                .build()
    }

    override fun onTerminate() {
        appComponent.locationKeeper.closeConnections()
        super.onTerminate()
    }
}