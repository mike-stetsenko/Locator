package stetsenko.locationtracker.repository.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

import android.arch.persistence.room.OnConflictStrategy.ABORT

@Dao
interface LocationDao {

    @get:Query("SELECT * FROM Location")
    val all: List<Location>

    @Insert(onConflict = ABORT)
    fun insertLocation(location: Location)

    @Query("DELETE FROM Location")
    fun deleteAll()
}
