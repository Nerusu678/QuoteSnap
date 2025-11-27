package uk.ac.tees.mad.quotesnap.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_posters")
data class SavedPoster(
    @PrimaryKey
    val id: String="",
    val quoteText: String="",
    val author: String="",
    val imageUrl: String="",  // Cloudinary URL
    val backgroundColor: String="#667eea",  // Store as hex string "#667eea"
    val textColor: String="#FFFFFF",
    val fontSize: Float=24f,
    val timestamp: Long = System.currentTimeMillis()
)
