package uk.ac.tees.mad.quotesnap.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.ac.tees.mad.quotesnap.data.ProfileRepository
import uk.ac.tees.mad.quotesnap.data.models.userData.UserPreferences
import uk.ac.tees.mad.quotesnap.data.models.userData.UserProfile
import uk.ac.tees.mad.quotesnap.utils.PosterImageGenerator
import uk.ac.tees.mad.quotesnap.utils.ZipHelper
import java.io.File
import javax.inject.Inject


@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {


    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true)
            }
            repository.getUserProfile()
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userProfile = profile
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
        }
    }

    fun updateProfile(fullName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.updateUserProfile(fullName)
                .onSuccess {
                    loadUserProfile()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
        }
    }

    fun deleteAccount(password:String,onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.deleteUserAccount(password)
//            onSuccess()
                .onSuccess {
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
        }
    }

    fun deleteAllPosters() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true)
            }
            repository.deleteAllPosters()
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun updatePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.updateUserPreferences(preferences)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userProfile = it.userProfile?.copy(preferences = preferences)
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {

                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                }
        }
    }


    fun exportAllPosters(context: Context, onSuccess: (File) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {

//                _uiState.update {
//                    it.copy(isLoading = true)
//                }

                // we will get all the posters
                val result = repository.getAllUserPosters()
                if (result.isFailure) {
                    throw result.exceptionOrNull() ?: Exception("Failed to get posters")
                }
                val posters = result.getOrNull() ?: emptyList()
                if (posters.isEmpty()) {
                    onError("No Posters to export")
//                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                // Generate images for each poster
                val imageFiles = mutableListOf<File>()
                posters.forEachIndexed { index, poster ->
                    val bitmap = PosterImageGenerator.generatePosterBitmap(poster)
                    val file = PosterImageGenerator.saveBitmapToFile(
                        context,
                        bitmap,
                        "poster_${index + 1}.png"
                    )
                    imageFiles.add(file)
                    bitmap.recycle()
                }

                // Create ZIP file
                val zipFile = ZipHelper.createZipFile(
                    context,
                    imageFiles,
                    "QuoteSnap_Posters_${System.currentTimeMillis()}.zip"
                )

                // Clean up temporary image files
                ZipHelper.cleanUpFiles(imageFiles)

//                _uiState.update { it.copy(isLoading = false) }
                onSuccess(zipFile)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Export error: ${e.message}")
//                _uiState.update { it.copy(isLoading = false) }
                onError(e.message ?: "Failed to export posters")
            }
        }
    }

    fun shareZipFile(context: Context, file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                android.content.Intent.createChooser(shareIntent, "Share Posters")
            )
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Share error: ${e.message}")
        }
    }

    // download zip file
    fun downloadZipFile(
        context: Context,
        file: File,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )

            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val destinationFile = File(downloadsDir, file.name)
            file.copyTo(destinationFile, overwrite = true)

            // Notify media scanner
            val intent =
                android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = android.net.Uri.fromFile(destinationFile)
            context.sendBroadcast(intent)

            onSuccess()
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Download error: ${e.message}")
            onError(e.message ?: "Failed to download")
        }
    }

}


data class ProfileUiState(
    val isLoading: Boolean = false,
//    val isExporting:Boolean=false,
    val userProfile: UserProfile? = null,
    val errorMessage: String? = null
)