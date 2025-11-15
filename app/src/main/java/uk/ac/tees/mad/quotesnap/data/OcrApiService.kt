package uk.ac.tees.mad.quotesnap.data

import okhttp3.MultipartBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import uk.ac.tees.mad.quotesnap.data.models.OcrResponse

interface OcrApiService {


    @Multipart  // uploading an image
    @POST("parse/image")
    suspend fun extractTextFromImage(
        @Header("apikey") apikey:String,
        @Query("language") language: String = "eng",
        @Query("isOverlayRequired") isOverlayRequired: Boolean = false,
        @Query("detectOrientation") detectOrientation: Boolean = true,
        @Query("scale") scale: Boolean = true,
        @Query("OCREngine") ocrEngine: Int = 2,
        @Part file: MultipartBody.Part
    ): OcrResponse


    companion object{
        const val BASE_URL="https://api.ocr.space/"

        const val API_KEY="K86066551388957"
    }
}