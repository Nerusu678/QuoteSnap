package uk.ac.tees.mad.quotesnap.data


import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import uk.ac.tees.mad.quotesnap.data.api.QuotableApi
import uk.ac.tees.mad.quotesnap.data.local.PosterDao
import uk.ac.tees.mad.quotesnap.data.local.SavedPoster
import uk.ac.tees.mad.quotesnap.data.models.quote.Quote
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

class QuoteSnapRepository @Inject constructor(
    private val quotableApi: QuotableApi,
    private val posterDao: PosterDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    // ============= QUOTE OPERATIONS =============

    /**
     * Get a random motivational quote
     */
    suspend fun getRandomQuote(): Result<Quote> {
        return try {
            val response = quotableApi.getRandomQuote(tags = "motivation")
            Result.success(response.toQuote())
        } catch (e: Exception) {
            Log.d("QSR", "getRandomQuote: " + e.message)
            Result.failure(e)
        }
    }

    /**
     * Get quote based on extracted OCR text keywords
     */
    suspend fun getQuoteForText(extractedText: String): Result<Quote> {
        return try {
            // Extract keywords from text
            val keywords = extractKeywords(extractedText)
            val tag = keywords.firstOrNull() ?: "motivational"

            Log.d("QSR", "getQuoteForText: " + tag)
            val response = quotableApi.getRandomQuote(tags = tag)
            Result.success(response.toQuote())
        } catch (e: Exception) {
            Log.d("QSR", "getQuoteForText: " + e.message)
            // Fallback to random motivational quote
            getRandomQuote()
        }
    }

    /**
     * Extract motivational keywords from text
     */
    private fun extractKeywords(text: String): List<String> {
        val motivationalWords = listOf(
            "success", "dream", "life", "wisdom", "inspirational",
            "motivational", "work", "love", "courage", "change",
            "future", "hope", "power", "believe", "goal"
        )

        val lowerText = text.lowercase()
        return motivationalWords.filter { word ->
            lowerText.contains(word)
        }
    }

    // ============= CLOUDINARY UPLOAD =============

    /**
     * Upload image to Cloudinary and return URL
     * Replace with your Cloudinary credentials
     */
    suspend fun uploadImageToCloudinary(bitmap: Bitmap): Result<String> {
        return try {
            // Convert bitmap to bytes
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            val imageBytes = baos.toByteArray()

            // Cloudinary credentials (REPLACE WITH YOURS)
            val cloudName = "dzyliedn1"  // TODO: Replace
            val uploadPreset = "unsigned_preset"  // TODO: Replace

            val url = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

            // Create multipart request
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", "poster.jpg",
                    imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .addFormDataPart("upload_preset", uploadPreset)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            // Execute request
            val client = OkHttpClient()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonObject = JSONObject(responseBody ?: "")
                val imageUrl = jsonObject.getString("secure_url")
                Result.success(imageUrl)
            } else {
                Result.failure(Exception("Upload failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============= SAVE POSTER =============

    /**
     * Complete save operation:
     * 1. Upload image to Cloudinary
     * 2. Save URL to Firestore
     * 3. Cache in Room database
     */
    suspend fun savePoster(
        bitmap: Bitmap,
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

            // 1. Upload to Cloudinary
            val uploadResult = uploadImageToCloudinary(bitmap)
            val imageUrl = uploadResult.getOrElse {
                return Result.failure(Exception("Image upload failed"))
            }

            // 2. Create poster object
            val poster = SavedPoster(
                id = posterId,
                quoteText = quoteText,
                author = author,
                imageUrl = imageUrl,  // Cloudinary URL
                backgroundColor = backgroundColor,
                textColor = textColor,
                fontSize = fontSize
            )

            // 3. Save to Firestore
            firestore.collection("users")
                .document(userId)
                .collection("posters")
                .document(posterId)
                .set(poster)
                .await()

            // 4. Cache in Room
            posterDao.insertPoster(poster)

            Result.success(posterId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============= GET POSTERS =============

    /**
     * Get all saved posters from local cache (Room)
     */
    fun getAllPosters(): Flow<List<SavedPoster>> {
        return posterDao.getAllPosters()
    }

    /**
     * Sync posters from Firestore to Room
     */
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

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============= DELETE POSTER =============

    /**
     * Delete poster from both Firestore and Room
     */
    suspend fun deletePoster(posterId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            // Delete from Firestore
            firestore.collection("users")
                .document(userId)
                .collection("posters")
                .document(posterId)
                .delete()
                .await()

            // Delete from Room
            posterDao.deletePoster(posterId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

            // Create poster object (no imageUrl needed)
            val poster = SavedPoster(
                id = posterId,
                quoteText = quoteText,
                author = author,
                imageUrl = "",  // Empty for now - we'll recreate the image when needed
                backgroundColor = backgroundColor,
                textColor = textColor,
                fontSize = fontSize
            )

            // Save to Firestore
            firestore.collection("users")
                .document(userId)
                .collection("posters")
                .document(posterId)
                .set(poster)
                .await()

            // Cache in Room
            posterDao.insertPoster(poster)

            Result.success(posterId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}