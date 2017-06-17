package stetsenko.locationtracker.di

import android.arch.persistence.room.Room
import android.content.Context
import dagger.Module
import dagger.Provides
import stetsenko.locationtracker.LocationKeeper
import stetsenko.locationtracker.repository.db.LocationDatabase
import stetsenko.locationtracker.repository.prefs.Prefs
import stetsenko.locationtracker.repository.prefs.PrefsImpl
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

    @Singleton
    @Provides
    fun provideDB(): LocationDatabase =
            Room.databaseBuilder(context, LocationDatabase::class.java, "LocationDatabase")
                    // TODO remove
                    // To simplify the codelab, allow queries on the main thread.
                    // Don't do this on a real app! See PersistenceBasicSample for an example.
                    .allowMainThreadQueries()
                    .build()

    @Singleton
    @Provides
    fun providePrefs(): Prefs = PrefsImpl(context)
}
