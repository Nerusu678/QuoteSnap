package uk.ac.tees.mad.quotesnap.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface PosterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoster(poster: SavedPoster)

    @Query("SELECT * FROM saved_posters ORDER BY timestamp DESC")
    fun getAllPosters(): Flow<List<SavedPoster>>

    @Query("DELETE FROM saved_posters WHERE id = :posterId")
    suspend fun deletePoster(posterId: String)
}