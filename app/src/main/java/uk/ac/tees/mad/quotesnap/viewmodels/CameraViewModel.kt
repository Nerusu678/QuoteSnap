package uk.ac.tees.mad.quotesnap.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import uk.ac.tees.mad.quotesnap.data.OcrApiService
import java.io.File
import javax.inject.Inject


@HiltViewModel
class CameraViewModel @Inject constructor(
    private val ocrApiService: OcrApiService
) : ViewModel() {

    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState = _cameraState.asStateFlow()

    fun processImage(imageUri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                _cameraState.update {
                    it.copy(
                        isProcessing = true,
                        errorMessage = null,
                        capturedImageUri = imageUri
                    )
                }


                // converting uri to file
                val file = File(imageUri.path ?: return@launch)
                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())

                val multipartBody = MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    requestBody
                )

                // now we will call ocr api
                val response = ocrApiService.extractTextFromImage(
                    apikey = OcrApiService.API_KEY,
                    file = multipartBody
                )

                // extracted text from the response
                val extractedText = response.parsedResults?.firstOrNull()?.parsedText

                // can we null or empty or can have ans
                if (extractedText.isNullOrEmpty()) {
                    _cameraState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "No text in the image. Please try again"
                        )
                    }
                } else {
                    _cameraState.update {
                        it.copy(
                            isProcessing = false,
                            isTextExtracted = true,
                            extractedText = extractedText
                        )
                    }
                }

            } catch (e: Exception) {
                _cameraState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Failed to extract text: ${e.message}"
                    )
                }
            }
        }
    }

    // reset the camera state
    fun resetCameraState() {
        _cameraState.value = CameraState()
    }

    // clear error
    fun clearError() {
        _cameraState.update {
            it.copy(errorMessage = null)
        }
    }


}

data class CameraState(
    val capturedImageUri: Uri? = null,
    val isProcessing: Boolean = false,
    val extractedText: String? = null,
    val isTextExtracted: Boolean = false,
    val errorMessage: String? = null
)