package uk.ac.tees.mad.quotesnap.data.models

import com.google.gson.annotations.SerializedName

data class ParsedResult(
    @SerializedName("ParsedText")
    val parsedText: String?,  // "MAKE TEXT\r\n" - This is what we want!

    @SerializedName("ErrorMessage")
    val errorMessage: String?,

    @SerializedName("ErrorDetails")
    val errorDetails: String?
)
