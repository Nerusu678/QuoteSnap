package uk.ac.tees.mad.quotesnap.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

    // Called when image is captured - just saves the URI
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

    // Called when user confirms to process the image
    fun processImage(context: Context) {
        val imageUri = _cameraState.value.capturedImageUri ?: return

        viewModelScope.launch {
            var tempFile: File? = null
            try {
                // Step 1: Update state to show loading (keep showPreview true)
                _cameraState.update {
                    it.copy(
                        isProcessing = true,
                        errorMessage = null
                    )
                }

                // Step 2: Convert URI to file and handle rotation
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: return@launch

                // Read image as bitmap
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                // ✅ FIX: Apply EXIF rotation to ensure text is right-side-up
                val rotatedBitmap = rotateImageIfRequired(context, imageUri, originalBitmap)

                // Recycle original if it was rotated
                if (rotatedBitmap !== originalBitmap) {
                    originalBitmap.recycle()
                }

                // Compress and resize image
                val compressedBitmap = compressImageForOCR(rotatedBitmap)

                // Create temporary file with iterative compression
                tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")

                // Start with quality 90 (less aggressive compression for better OCR)
                var quality = 90
                var fileSize: Long

                do {
                    tempFile.outputStream().use { outputStream ->
                        compressedBitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            quality,
                            outputStream
                        )
                    }
                    fileSize = tempFile.length()

                    // If still too large, reduce quality
                    if (fileSize > 1024 * 1024 && quality > 60) {
                        quality -= 10
                        android.util.Log.d("OCR_DEBUG", "File too large (${fileSize / 1024}KB), reducing quality to $quality")
                    } else {
                        break
                    }
                } while (fileSize > 1024 * 1024 && quality >= 60)

                // Log file details for debugging
                android.util.Log.d("OCR_DEBUG", "Final file size: ${tempFile.length() / 1024} KB")
                android.util.Log.d("OCR_DEBUG", "Final quality: $quality%")
                android.util.Log.d("OCR_DEBUG", "Image dimensions: ${compressedBitmap.width}x${compressedBitmap.height}")

                // Recycle bitmaps to free memory
                rotatedBitmap.recycle()
                compressedBitmap.recycle()

                // Prepare multipart body for API
                val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData(
                    "file",
                    tempFile.name,
                    requestBody
                )

                android.util.Log.d("OCR_DEBUG", "Sending OCR request...")

                // Step 3: Call OCR API with proper parameters
                val response = ocrApiService.extractTextFromImage(
                    apikey = OcrApiService.API_KEY,
                    language = "eng",
                    isOverlayRequired = false,
                    detectOrientation = true,
                    scale = true,
                    ocrEngine = 2,
                    file = multipartBody
                )

                android.util.Log.d("OCR_DEBUG", "OCR Response: ${response.parsedResults?.size ?: 0} results")
                android.util.Log.d("OCR_DEBUG", "OCR Status: ${response.isErroredOnProcessing}")
                android.util.Log.d("OCR_DEBUG", "Error Message: ${response.errorMessage}")
                response.parsedResults?.firstOrNull()?.let { result ->
                    android.util.Log.d("OCR_DEBUG", "Parsed Text Length: ${result.parsedText?.length ?: 0}")
                    android.util.Log.d("OCR_DEBUG", "First 100 chars: ${result.parsedText?.take(100)}")
                }

                // Step 4: Extract text from response
                val extractedText = response.parsedResults?.firstOrNull()?.parsedText

                // Step 5: Update state with result
                if (extractedText.isNullOrEmpty()) {
                    // Check if API returned an error
                    val errorMsg = if (response.isErroredOnProcessing == true) {
                        "OCR Error: ${response.errorMessage ?: response.errorMessage ?: "Unknown error"}"
                    } else {
                        "No text found in the image. Please try again with clearer text"
                    }

                    android.util.Log.w("OCR_DEBUG", "No text extracted: $errorMsg")

                    _cameraState.update {
                        it.copy(
                            isProcessing = false,
                            showPreview = false,
                            errorMessage = errorMsg
                        )
                    }
                } else {
                    android.util.Log.d("OCR_DEBUG", "✅ Text extracted successfully")
                    _cameraState.update {
                        it.copy(
                            isProcessing = false,
                            showPreview = false,
                            isTextExtracted = true,
                            extractedText = extractedText.trim()
                        )
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("OCR_DEBUG", "Exception: ${e.message}", e)
                _cameraState.update {
                    it.copy(
                        isProcessing = false,
                        showPreview = false,
                        errorMessage = "Failed to extract text: ${e.message}"
                    )
                }
            } finally {
                // Cleanup: Always delete temporary file
                tempFile?.delete()
            }
        }
    }

    // Reset the camera state
    fun resetCameraState() {
        _cameraState.value = CameraState()
    }

    // Clear error
    fun clearError() {
        _cameraState.update {
            it.copy(errorMessage = null)
        }
    }

    // Compress image to under 1MB while maintaining readability for OCR
    private fun compressImageForOCR(bitmap: Bitmap): Bitmap {
        // ✅ Better dimensions for OCR (balance between size and readability)
        val maxWidth = 2048  // Increased for better text clarity
        val maxHeight = 2048

        val width = bitmap.width
        val height = bitmap.height

        // Calculate scale to fit within max dimensions
        val scale = minOf(
            if (width > maxWidth) maxWidth.toFloat() / width else 1f,
            if (height > maxHeight) maxHeight.toFloat() / height else 1f
        )

        // Only scale down if needed
        return if (scale < 1f) {
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    // ✅ NEW: Rotate image according to EXIF orientation
    private fun rotateImageIfRequired(context: Context, imageUri: Uri, bitmap: Bitmap): Bitmap {
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: return bitmap

        val exif = try {
            ExifInterface(inputStream)
        } catch (e: Exception) {
            android.util.Log.e("OCR_DEBUG", "Error reading EXIF: ${e.message}")
            inputStream.close()
            return bitmap
        }

        inputStream.close()

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        android.util.Log.d("OCR_DEBUG", "EXIF Orientation: $orientation")

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                android.util.Log.d("OCR_DEBUG", "Rotating image 90°")
                rotateImage(bitmap, 90f)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                android.util.Log.d("OCR_DEBUG", "Rotating image 180°")
                rotateImage(bitmap, 180f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                android.util.Log.d("OCR_DEBUG", "Rotating image 270°")
                rotateImage(bitmap, 270f)
            }
            else -> {
                android.util.Log.d("OCR_DEBUG", "No rotation needed")
                bitmap
            }
        }
    }

    // Helper to rotate bitmap
    private fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
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