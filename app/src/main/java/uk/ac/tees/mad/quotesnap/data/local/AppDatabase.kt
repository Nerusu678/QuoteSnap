package uk.ac.tees.mad.quotesnap.data.local

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(
    entities = [SavedPoster::class],
    version = 1,
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun posterDao(): PosterDao
}