package uk.ac.tees.mad.quotesnap.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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


//      Get quote based on extracted OCR text keywords
//      Falls back to random quote if API fails

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
//        val motivationalWords = listOf(
//            "success", "dream", "life", "wisdom", "inspirational",
//            "motivational", "work", "love", "courage", "change",
//            "future", "hope", "power", "believe", "goal"
//        )

        val motivationalWords=listOf(
            "age",
            "athletics",
            "business",
            "change",
            "character",
            "competition",
            "conservative",
            "courage",
            "creativity",
            "education",
            "ethics",
            "failure",
            "faith",
            "family",
            "famous-quotes",
            "film",
            "freedom",
            "friendship",
            "future",
            "generosity",
            "genius",
            "gratitude",
            "happiness",
            "health",
            "history",
            "honor",
            "humor",
            "humorous",
            "imagination",
            "inspirational",
            "knowledge",
            "leadership",
            "life",
            "literature",
            "love",
            "mathematics",
            "motivational",
            "nature",
            "opportunity",
            "pain",
            "perseverance",
            "philosophy",
            "politics",
            "power-quotes",
            "proverb",
            "religion",
            "sadness",
            "science",
            "self",
            "self-help",
            "social-justice",
            "society",
            "spirituality",
            "sports",
            "stupidity",
            "success",
            "technology",
            "time",
            "tolerance",
            "truth",
            "virtue",
            "war",
            "weakness",
            "wellness",
            "wisdom",
            "work"
        )


        val lowerText = text.lowercase()
        return motivationalWords.filter { word -> lowerText.contains(word) }
    }

    // ============= SAVE POSTER =============

    //      Save poster design to Firestore and Room
//      No image upload recreate the poster from design data
    suspend fun saveDesignOnly(
        quoteText: String,
        author: String,
        backgroundColor: String,
        textColor: String,
        fontSize: Float,
        fontFamily: String,
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
                fontSize = fontSize,
                fontFamily = fontFamily
            )
            // Save to Room (local cache for offline access)
            posterDao.insertPoster(poster)

            // Save to Firestore
            val posterMap = hashMapOf(
                "id" to poster.id,
                "quoteText" to poster.quoteText,
                "author" to poster.author,
                "imageUrl" to poster.imageUrl,
                "backgroundColor" to poster.backgroundColor,
                "textColor" to poster.textColor,
                "fontSize" to poster.fontSize,
                "fontFamily" to poster.fontFamily, // NEW: Include in Firebase
                "timestamp" to poster.timestamp
            )

            // Save to Firestore (cloud backup)
            firestore.collection("users")
                .document(userId)
                .collection("posters")
                .document(posterId)
                .set(posterMap)
                .await()


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
        return withContext(Dispatchers.IO) {
            try {
                val userId = auth.currentUser?.uid
                    ?: return@withContext Result.failure(Exception("User not logged in"))

                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("posters")
                    .get()
                    .await()

                val posters = snapshot.documents.mapNotNull { doc ->
                    try {
                        SavedPoster(
                            id = doc.getString("id") ?: "",
                            quoteText = doc.getString("quoteText") ?: "",
                            author = doc.getString("author") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            backgroundColor = doc.getString("backgroundColor") ?: "#667eea",
                            textColor = doc.getString("textColor") ?: "#FFFFFF",
                            fontSize = doc.getDouble("fontSize")?.toFloat() ?: 24f,
                            fontFamily = doc.getString("fontFamily")
                                ?: "SANS_SERIF", // NEW: Get font
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        Log.e("Repository", "Failed to parse poster: ${e.message}")
                        null
                    }
                }
                posters.forEach {poster->
                    posterDao.insertPoster(poster)
                }
//                snapshot.documents.forEach { doc ->
//                    val poster = doc.toObject(SavedPoster::class.java)
//                    poster?.let { posterDao.insertPoster(it) }
//                }
                // Insert all posters into local database
//                posterDao.insertPosters(posters)

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("Repository", "Sync failed: ${e.message}")
                Result.failure(e)
            }
        }
//        return try {
//            val userId = auth.currentUser?.uid
//                ?: return Result.failure(Exception("User not logged in"))
//
//            val snapshot = firestore.collection("users")
//                .document(userId)
//                .collection("posters")
//                .get()
//                .await()
//
//            snapshot.documents.forEach { doc ->
//                val poster = doc.toObject(SavedPoster::class.java)
//                poster?.let { posterDao.insertPoster(it) }
//            }
//
//            Log.d("QuoteSnapRepository", "Synced ${snapshot.size()} posters from Firestore")
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Log.e("QuoteSnapRepository", "Sync error: ${e.message}")
//            Result.failure(e)
//        }
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