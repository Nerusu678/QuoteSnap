package uk.ac.tees.mad.quotesnap.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
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

    fun onImageCaptured(imageUri: Uri) {
        _cameraState.update {
            it.copy(
                capturedImageUri = imageUri,
                showPreview = true,
                isProcessing = false,
                errorMessage = null
            )
        }
    }

    fun processImage(context: Context) {
        val imageUri = _cameraState.value.capturedImageUri ?: run {
            _cameraState.update { it.copy(errorMessage = "No image to process") }
            return
        }

        viewModelScope.launch {
            var tempFile: File? = null
            try {
                _cameraState.update { it.copy(isProcessing = true, errorMessage = null) }

                // Read and process image
                val inputStream = context.contentResolver.openInputStream(imageUri)
                if (inputStream == null) {
                    _cameraState.update {
                        it.copy(
                            isProcessing = false,
                            showPreview = true,  // ✅ CHANGED: Keep preview
                            errorMessage = "Failed to read image file"
                        )
                    }
                    return@launch
                }

                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (originalBitmap == null) {
                    _cameraState.update {
                        it.copy(
                            isProcessing = false,
                            showPreview = true,  // ✅ CHANGED: Keep preview
                            errorMessage = "Invalid image format"
                        )
                    }
                    return@launch
                }

                // Apply EXIF rotation for OCR accuracy
                val rotatedBitmap = applyExifRotation(context, imageUri, originalBitmap)
                if (rotatedBitmap !== originalBitmap) {
                    originalBitmap.recycle()
                }

                // Compress image for API (max 1MB, 2048x2048)
                val compressedBitmap = resizeBitmap(rotatedBitmap, 2048, 2048)
                rotatedBitmap.recycle()

                // Save to temp file with quality adjustment
                tempFile = File(context.cacheDir, "ocr_${System.currentTimeMillis()}.jpg")
                var quality = 90

                do {
                    tempFile.outputStream().use { out ->
                        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    }
                    if (tempFile.length() > 1_048_576 && quality > 60) {
                        quality -= 10
                    } else break
                } while (quality >= 60)

                compressedBitmap.recycle()

                // Call OCR API
                val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val multipartBody =
                    MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

                val response = ocrApiService.extractTextFromImage(
                    apikey = OcrApiService.API_KEY,
                    language = "eng",
                    isOverlayRequired = false,
                    detectOrientation = true,
                    scale = true,
                    ocrEngine = 2,
                    file = multipartBody
                )

                // Handle response
                val extractedText = response.parsedResults?.firstOrNull()?.parsedText

                if (extractedText.isNullOrEmpty()) {
                    val errorMsg = if (response.isErroredOnProcessing == true) {
                        "OCR Error: ${response.errorMessage ?: "Unknown error"}"
                    } else {
                        "No text found. Please try again with clearer text"
                    }
                    _cameraState.update {
                        it.copy(
                            isProcessing = false,
                            showPreview = true,  // ✅ CHANGED: Keep preview
                            errorMessage = errorMsg
                        )
                    }
                } else {
                    _cameraState.update {
                        it.copy(
                            isProcessing = false,
                            showPreview = false,  // ✅ CORRECT: Hide on success
                            isTextExtracted = true,
                            extractedText = extractedText.trim()
                        )
                    }
                }

            } catch (e: java.net.SocketTimeoutException) {
                _cameraState.update {
                    it.copy(
                        isProcessing = false,
                        showPreview = true,  // ✅ CHANGED: Keep preview
                        errorMessage = "Connection timed out. Please check your internet and try again."
                    )
                }
            } catch (e: java.net.UnknownHostException) {
                _cameraState.update {
                    it.copy(
                        isProcessing = false,
                        showPreview = true,  // ✅ CHANGED: Keep preview
                        errorMessage = "No internet connection. Please check your network."
                    )
                }
            } catch (e: java.io.IOException) {
                _cameraState.update {
                    it.copy(
                        isProcessing = false,
                        showPreview = true,  // ✅ CHANGED: Keep preview
                        errorMessage = "Failed to read image: ${e.localizedMessage}"
                    )
                }
            } catch (e: Exception) {
                _cameraState.update {
                    it.copy(
                        isProcessing = false,
                        showPreview = true,  // ✅ CHANGED: Keep preview
                        errorMessage = "Failed to extract text: ${e.localizedMessage}"
                    )
                }
            } finally {
                tempFile?.delete()
            }
        }
    }


//    fun processImage(context: Context) {
//        val imageUri = _cameraState.value.capturedImageUri ?: return
//
//        viewModelScope.launch {
//            var tempFile: File? = null
//            try {
//                _cameraState.update { it.copy(isProcessing = true, errorMessage = null) }
//
//                // Read and process image
//                val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@launch
//                val originalBitmap = BitmapFactory.decodeStream(inputStream)
//                inputStream.close()
//
//                // Apply EXIF rotation for OCR accuracy
//                val rotatedBitmap = applyExifRotation(context, imageUri, originalBitmap)
//                if (rotatedBitmap !== originalBitmap) {
//                    originalBitmap.recycle()
//                }
//
//                // Compress image for API (max 1MB, 2048x2048)
//                val compressedBitmap = resizeBitmap(rotatedBitmap, 2048, 2048)
//                rotatedBitmap.recycle()
//
//                // Save to temp file with quality adjustment
//                tempFile = File(context.cacheDir, "ocr_${System.currentTimeMillis()}.jpg")
//                var quality = 90
//
//                do {
//                    tempFile.outputStream().use { out ->
//                        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
//                    }
//                    if (tempFile.length() > 1_048_576 && quality > 60) {
//                        quality -= 10
//                    } else break
//                } while (quality >= 60)
//
//                compressedBitmap.recycle()
//
//                // Call OCR API
//                val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
//                val multipartBody = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)
//
//                val response = ocrApiService.extractTextFromImage(
//                    apikey = OcrApiService.API_KEY,
//                    language = "eng",
//                    isOverlayRequired = false,
//                    detectOrientation = true,
//                    scale = true,
//                    ocrEngine = 2,
//                    file = multipartBody
//                )
//
//                // Handle response
//                val extractedText = response.parsedResults?.firstOrNull()?.parsedText
//
//                if (extractedText.isNullOrEmpty()) {
//                    val errorMsg = if (response.isErroredOnProcessing == true) {
//                        "OCR Error: ${response.errorMessage ?: "Unknown error"}"
//                    } else {
//                        "No text found. Please try again with clearer text"
//                    }
//                    _cameraState.update {
//                        it.copy(isProcessing = false, showPreview = false, errorMessage = errorMsg)
//                    }
//                } else {
//                    _cameraState.update {
//                        it.copy(
//                            isProcessing = false,
//                            showPreview = false,
//                            isTextExtracted = true,
//                            extractedText = extractedText.trim()
//                        )
//                    }
//                }
//
//            } catch (e: Exception) {
//                _cameraState.update {
//                    it.copy(
//                        isProcessing = false,
//                        showPreview = false,
//                        errorMessage = "Failed to extract text: ${e.localizedMessage}"
//                    )
//                }
//            } finally {
//                tempFile?.delete()
//            }
//        }
//    }

    fun resetCameraState() {
        _cameraState.value = CameraState()
    }

    fun clearError() {
        _cameraState.update { it.copy(errorMessage = null) }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val scale = minOf(
            if (bitmap.width > maxWidth) maxWidth.toFloat() / bitmap.width else 1f,
            if (bitmap.height > maxHeight) maxHeight.toFloat() / bitmap.height else 1f
        )

        return if (scale < 1f) {
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    private fun applyExifRotation(context: Context, imageUri: Uri, bitmap: Bitmap): Bitmap {
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: return bitmap

        val exif = try {
            ExifInterface(inputStream)
        } catch (e: Exception) {
            inputStream.close()
            return bitmap
        }
        inputStream.close()

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }

        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

data class CameraState(
    val capturedImageUri: Uri? = null,
    val showPreview: Boolean = false,
    val isProcessing: Boolean = false,
    val extractedText: String? = null,
    val isTextExtracted: Boolean = false,
    val errorMessage: String? = null
)