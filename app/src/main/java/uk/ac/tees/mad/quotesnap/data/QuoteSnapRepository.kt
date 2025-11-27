package uk.ac.tees.mad.quotesnap.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import uk.ac.tees.mad.quotesnap.data.api.QuotableApi
import uk.ac.tees.mad.quotesnap.data.local.PosterDao
import uk.ac.tees.mad.quotesnap.data.local.SavedPoster
import uk.ac.tees.mad.quotesnap.data.models.quote.Quote
import java.util.UUID
import javax.inject.Inject

class QuoteSnapRepository @Inject constructor(
    private val quotableApi: QuotableApi,
    private val posterDao: PosterDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {


//

//      Get a random motivational quote from API

    suspend fun getRandomQuote(): Result<Quote> {
        return try {
            val response = quotableApi.getRandomQuote(tags = "motivational")
            Result.success(response.toQuote())
        } catch (e: Exception) {
            Log.d("QuoteSnapRepository", "getRandomQuote error: ${e.message}")
            Result.failure(e)
        }
    }


//     * Get quote based on extracted OCR text keywords
//     * Falls back to random quote if API fails

    suspend fun getQuoteForText(extractedText: String): Result<Quote> {
        return try {
            val keywords = extractKeywords(extractedText)
            val tag = keywords.firstOrNull() ?: "motivational"

            Log.d("QuoteSnapRepository", "Searching quote with tag: $tag")
            val response = quotableApi.getRandomQuote(tags = tag)
            Result.success(response.toQuote())
        } catch (e: Exception) {
            Log.d("QuoteSnapRepository", "getQuoteForText error: ${e.message}")
            // Fallback to random quote
            getRandomQuote()
        }
    }

//     * Extract motivational keywords from text
    private fun extractKeywords(text: String): List<String> {
        val motivationalWords = listOf(
            "success", "dream", "life", "wisdom", "inspirational",
            "motivational", "work", "love", "courage", "change",
            "future", "hope", "power", "believe", "goal"
        )

        val lowerText = text.lowercase()
        return motivationalWords.filter { word -> lowerText.contains(word) }
    }

    // ============= SAVE POSTER =============

//     * Save poster design to Firestore and Room
//     * No image upload - we recreate the poster from design data
    suspend fun saveDesignOnly(
        quoteText: String,
        author: String,
        backgroundColor: String,
        textColor: String,
        fontSize: Float
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val posterId = UUID.randomUUID().toString()

            val poster = SavedPoster(
                id = posterId,
                quoteText = quoteText,
                author = author,
                imageUrl = "",  // No image - recreate from design data
                backgroundColor = backgroundColor,
                textColor = textColor,
                fontSize = fontSize
            )

            // Save to Firestore (cloud backup)
            firestore.collection("users")
                .document(userId)
                .collection("posters")
                .document(posterId)
                .set(poster)
                .await()

            // Save to Room (local cache for offline access)
            posterDao.insertPoster(poster)

            Log.d("QuoteSnapRepository", "Poster saved successfully: $posterId")
            Result.success(posterId)
        } catch (e: Exception) {
            Log.e("QuoteSnapRepository", "Save poster error: ${e.message}")
            Result.failure(e)
        }
    }



//     * Get all saved posters from local cache (Room)
//     * Returns Flow for automatic UI updates
    fun getAllPosters(): Flow<List<SavedPoster>> {
        return posterDao.getAllPosters()
    }


//     * Sync posters from Firestore to Room
//     * Useful when user logs in on a new device
    suspend fun syncPostersFromFirestore(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("posters")
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                val poster = doc.toObject(SavedPoster::class.java)
                poster?.let { posterDao.insertPoster(it) }
            }

            Log.d("QuoteSnapRepository", "Synced ${snapshot.size()} posters from Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("QuoteSnapRepository", "Sync error: ${e.message}")
            Result.failure(e)
        }
    }


//     * Delete poster from both Firestore and Room
    suspend fun deletePoster(posterId: String): Result<Unit> {
        return try {
            // Delete from Room (local)
            posterDao.deletePoster(posterId)
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            // Delete from Firestore (cloud)
            firestore.collection("users")
                .document(userId)
                .collection("posters")
                .document(posterId)
                .delete()
                .await()


            Log.d("QuoteSnapRepository", "Poster deleted: $posterId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("QuoteSnapRepository", "Delete error: ${e.message}")
            Result.failure(e)
        }
    }
}