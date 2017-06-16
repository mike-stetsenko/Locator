package stetsenko.locationtracker.di

import android.content.Context
import dagger.Module
import dagger.Provides
import stetsenko.locationtracker.LocationKeeper
import javax.inject.Singleton

@Module
class AppModule(context: Context) {

    private val context: Context = context.applicationContext

    @Singleton
    @Provides
    fun provideAppContext(): Context = context

    @Singleton
    @Provides
    fun provideLocationKeeper(): LocationKeeper = LocationKeeper()
}
