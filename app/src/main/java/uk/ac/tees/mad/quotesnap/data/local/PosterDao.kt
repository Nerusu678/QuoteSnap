package uk.ac.tees.mad.quotesnap.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface PosterDao {


    // insert poster
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoster(poster: SavedPoster)


    // get all the posters
    @Query("SELECT * FROM saved_posters ORDER BY timestamp DESC")
    fun getAllPosters(): Flow<List<SavedPoster>>

    // delete with poster id
    @Query("DELETE FROM saved_posters WHERE id = :posterId")
    suspend fun deletePoster(posterId: String)

    // delete all posters
    @Query("delete from saved_posters")
    suspend fun deleteAllPosters()
}