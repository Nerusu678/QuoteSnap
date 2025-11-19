package uk.ac.tees.mad.quotesnap.data.api

import retrofit2.http.GET
import retrofit2.http.Query
import uk.ac.tees.mad.quotesnap.data.models.quote.QuotableResponse

interface QuotableApi {

    @GET("random")
    suspend fun getRandomQuote(
        @Query("tags") tags: String = "motivation",
        @Query("maxLength") maxLength: Int = 150
    ): QuotableResponse

    companion object {
        const val BASE_URL = "https://api.quotable.io/"
    }
}