package uk.ac.tees.mad.quotesnap.data.models.ocr

import com.google.gson.annotations.SerializedName

// OCR API Response
data class OcrResponse(
    @SerializedName("ParsedResults")
    val parsedResults: List<ParsedResult>?,

    @SerializedName("OCRExitCode")
    val ocrExitCode: Int,  // 1 = Success, 2 = Failed

    @SerializedName("IsErroredOnProcessing")
    val isErroredOnProcessing: Boolean,

    @SerializedName("ErrorMessage")
    val errorMessage: List<String>?
)
