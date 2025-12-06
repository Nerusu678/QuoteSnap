package uk.ac.tees.mad.quotesnap.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import uk.ac.tees.mad.quotesnap.data.local.PosterDao
import uk.ac.tees.mad.quotesnap.data.local.SavedPoster
import uk.ac.tees.mad.quotesnap.data.models.userData.UserPreferences
import uk.ac.tees.mad.quotesnap.data.models.userData.UserProfile
import javax.inject.Inject


class ProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val posterDao: PosterDao
) {

    suspend fun createUserProfile(
        userId: String,
        fullName: String,
        email: String
    ): Result<Unit> {
        return try {
            val userProfile = UserProfile(
                userId = userId,
                fullName=fullName,
                email = email
            )
            firestore.collection("users")
                .document(userId)
                .set(userProfile)
                .await()
            Log.d("PR", "createUserProfile: $userId ")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Create profile error: ${e.message}")
            Result.failure(e)
        }

    }

    suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val snapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val profile = snapshot.toObject(UserProfile::class.java)
                ?: return Result.failure(Exception("Profile not found"))

            Log.d("ProfileRepository", "Profile fetched: ${profile.fullName}")
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(fullName: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "fullName" to fullName,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()

            Log.d("ProfileRepository", "Profile updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Update profile error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccount(password:String): Result<Unit> {
        return try {

            val email=auth.currentUser!!.email!!
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))


            val credential = EmailAuthProvider.getCredential(email, password)
            auth.currentUser!!.reauthenticate(credential).await()

            // Delete all posters from Firestore
            val postersSnapshot = firestore.collection("users")
                .document(userId)
                .collection("posters")
                .get()
                .await()

            postersSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            // Delete user profile from Firestore
            firestore.collection("users")
                .document(userId)
                .delete()
                .await()

            // Delete from Room database
            posterDao.deleteAllPosters()

            // Delete Firebase Auth account
            auth.currentUser?.delete()?.await()

            Log.d("ProfileRepository", "Account deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Delete account error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "preferences" to preferences,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()

            Log.d("ProfileRepository", "Preferences updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Update preferences error: ${e.message}")
            Result.failure(e)
        }
    }

    // get all the posters


//     * Get all posters for current user
    suspend fun getAllUserPosters(): Result<List<SavedPoster>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("posters")
                .get()
                .await()

            val posters = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SavedPoster::class.java)
            }

            Log.d("ProfileRepository", "Fetched ${posters.size} posters")
            Result.success(posters)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Get posters error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteAllPosters():Result<Unit>{
        return try{
            posterDao.deleteAllPosters()
            Result.success(Unit)
        }catch (e: Exception){
            Log.e("ProfileRepository", "Get posters error: ${e.message}")
            Result.failure(e)
        }

    }

}